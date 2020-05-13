drop table debug_table;

create table debug_table(
d timestamp
);

truncate table debug_table;

create or replace package body pkg_test is

procedure set_global_context(param1 in number,param2 in date) is
  pragma autonomous_transaction;
begin
 insert into debug_table values(SYSTIMESTAMP);
 commit;
end;

....

select *
from debug_table
order by 1

D
14-MAY-20 01.22.02.726418 AM
14-MAY-20 01.22.02.872519 AM
14-MAY-20 01.22.02.873623 AM
14-MAY-20 01.22.02.876562 AM
14-MAY-20 01.22.02.877433 AM
14-MAY-20 01.22.02.888567 AM
14-MAY-20 01.22.02.893415 AM
14-MAY-20 01.22.02.897361 AM
14-MAY-20 01.22.02.906791 AM
14-MAY-20 01.22.02.907763 AM
14-MAY-20 01.22.02.909156 AM
