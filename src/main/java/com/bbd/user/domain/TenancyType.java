package com.bbd.user.domain;

/*
 UserSnapshot에서 데이터 접근 범위를 판단할 소속 타입.

 HQ 사용자인지, BRANCH 사용자인지에 따라
 각 MSA의 조회/수정 가능 데이터 범위가 달라질 수 있다.
 */
public enum TenancyType {
    HQ,
    BRANCH
}