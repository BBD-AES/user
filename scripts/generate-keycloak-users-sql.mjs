import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

const [, , sourceArgument, outputArgument] = process.argv;

if (!sourceArgument || !outputArgument) {
    throw new Error(
        "Usage: node scripts/generate-keycloak-users-sql.mjs <keycloak-users.json> <output.sql>"
    );
}

const sourcePath = resolve(sourceArgument);
const outputPath = resolve(outputArgument);
const users = JSON.parse(await readFile(sourcePath, "utf8"));

const sqlString = (value) => {
    if (value == null || value === "") {
        return "NULL";
    }

    return `'${String(value).replaceAll("'", "''")}'`;
};

const rows = users.map((user) => {
    const attributes = user.attributes ?? {};
    const first = (name) =>
        Array.isArray(attributes[name]) ? attributes[name][0] : null;

    const role = first("role");
    const employmentStatus = first("employment_status");

    /*
     Keycloak의 재직 ACTIVE와 ERP 사용 승인은 서로 다른 상태다.
     초기 시스템 관리자는 바로 사용할 수 있게 ACTIVE로 두고,
     나머지 재직자는 ERP 승인 전 PENDING으로 적재한다.
     */
    const status = !user.enabled || employmentStatus !== "ACTIVE"
        ? "INACTIVE"
        : role === "ADMIN"
            ? "ACTIVE"
            : "PENDING";

    return {
        keycloakSub: user.id,
        employeeNumber: first("employee_number"),
        username: user.username,
        displayName: first("name"),
        email: user.email ?? null,
        position: first("position"),
        status,
        role,
        tenancyType: first("tenancy_type"),
        tenancyName: first("tenancy_name")
    };
});

const requiredFields = [
    "keycloakSub",
    "employeeNumber",
    "username",
    "displayName",
    "position",
    "role",
    "tenancyType",
    "tenancyName"
];

for (const [index, row] of rows.entries()) {
    for (const field of requiredFields) {
        if (row[field] == null || row[field] === "") {
            throw new Error(`User index ${index} is missing required field: ${field}`);
        }
    }
}

const allowedRoles = new Set([
    "ADMIN",
    "HQ_MANAGER",
    "HQ_STAFF",
    "BRANCH_MANAGER",
    "BRANCH_STAFF"
]);
const allowedTenancyTypes = new Set(["HQ", "BRANCH"]);

for (const row of rows) {
    if (!allowedRoles.has(row.role)) {
        throw new Error(`Unsupported role: ${row.role}`);
    }

    if (!allowedTenancyTypes.has(row.tenancyType)) {
        throw new Error(`Unsupported tenancy type: ${row.tenancyType}`);
    }
}

const duplicate = (field) =>
    rows.find((row, index) =>
        rows.findIndex((candidate) => candidate[field] === row[field]) !== index
    );

if (duplicate("keycloakSub")) {
    throw new Error("Duplicate keycloakSub exists in source JSON");
}

if (duplicate("employeeNumber")) {
    throw new Error("Duplicate employeeNumber exists in source JSON");
}

const values = rows
    .map((row) => `    (${[
        sqlString(row.keycloakSub),
        sqlString(row.employeeNumber),
        sqlString(row.username),
        sqlString(row.displayName),
        sqlString(row.email),
        sqlString(row.position),
        sqlString(row.status),
        sqlString(row.role),
        sqlString(row.tenancyType),
        sqlString(row.tenancyName)
    ].join(", ")})`)
    .join(",\n");

const sql = `-- Keycloak 사용자 ${rows.length}명을 ERP users 테이블에 최초 적재하는 bootstrap SQL.
-- 생성 원본: ${sourcePath.replaceAll("\\", "/")}
--
-- 상태 매핑 정책:
-- 1. ADMIN은 ERP 초기 관리자이므로 ACTIVE + ADMIN으로 적재한다.
-- 2. 그 외 재직 ACTIVE 사용자는 ERP 승인 전 상태인 PENDING으로 적재한다.
-- 3. ON_LEAVE, SUSPENDED, RESIGNED 또는 Keycloak disabled 사용자는 INACTIVE로 적재한다.
--
-- 이 SQL은 초기 데이터 적재용이다. application service를 거치지 않으므로 user_outbox를 만들지 않는다.
-- Redis가 비어 있는 최초 구축에서는 문제가 없지만, 운영 중 반복 동기화에는 SCIM/API를 사용해야 한다.

BEGIN;

MERGE INTO users AS target
USING (
    VALUES
${values}
) AS source (
    keycloak_sub, employee_number, username, display_name, email, position,
    status, role, tenancy_type, tenancy_name
)
ON target.employee_number = source.employee_number
   OR target.keycloak_sub = source.keycloak_sub
WHEN MATCHED AND (
    target.keycloak_sub,
    target.employee_number,
    target.username,
    target.display_name,
    target.email,
    target.position,
    target.status,
    target.role,
    target.tenancy_type,
    target.tenancy_name
) IS DISTINCT FROM (
    source.keycloak_sub,
    source.employee_number,
    source.username,
    source.display_name,
    source.email,
    source.position,
    source.status,
    source.role,
    source.tenancy_type,
    source.tenancy_name
) THEN
    UPDATE SET
        keycloak_sub = source.keycloak_sub,
        employee_number = source.employee_number,
        username = source.username,
        display_name = source.display_name,
        email = source.email,
        position = source.position,
        status = source.status,
        role = source.role,
        tenancy_type = source.tenancy_type,
        tenancy_name = source.tenancy_name,
        version = target.version + 1,
        updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (
        keycloak_sub, employee_number, username, display_name, email, position,
        status, role, tenancy_type, tenancy_name, version, created_at, updated_at
    )
    VALUES (
        source.keycloak_sub, source.employee_number, source.username, source.display_name,
        source.email, source.position, source.status, source.role, source.tenancy_type,
        source.tenancy_name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

COMMIT;

-- 적재 결과 확인.
SELECT status, role, COUNT(*) AS user_count
FROM users
GROUP BY status, role
ORDER BY status, role;

SELECT id, keycloak_sub, employee_number, username, display_name, status, role,
       tenancy_type, tenancy_name, version
FROM users
ORDER BY employee_number;
`;

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, sql, "utf8");

console.log(`Generated ${rows.length} users: ${outputPath}`);
