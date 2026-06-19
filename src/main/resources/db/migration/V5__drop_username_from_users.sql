DROP INDEX IF EXISTS public.uk_users_username;

ALTER TABLE public.users
DROP COLUMN IF EXISTS username;