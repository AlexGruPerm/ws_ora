clear;

create user WSORA
  identified by WSORA
  default tablespace USERS
  temporary tablespace TEMP_NEW
  profile DEFAULT
  quota unlimited on users;

grant select on SYS.V_$MYSTAT to WSORA;
grant select on SYS.V_$SESSTAT to WSORA;
grant select on SYS.V_$STATNAME to WSORA;

grant connect to WSORA;
grant resource to WSORA;

alter user WSORA
  default role connect, resource;