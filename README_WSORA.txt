/*
begin
   for ds in (select 'ALTER SYSTEM KILL SESSION '''||s.SID||','||s.SERIAL#||''' IMMEDIATE' as ks
              from v$session s
              where s.USERNAME='MSK_ARM_LEAD'
                and s.OSUSER='yakushev'
                and s.MACHINE='PRM-WS-0006')
   loop
    execute immediate ds.ks;
   end loop;
end;
*/
select s.SID,
       s.PROCESS,
       s.PORT,
       s.TERMINAL,
       s.PROGRAM,
       s.ACTION,
       s.module,
       s.STATE,
       s.STATUS
from v$session s
where s.USERNAME='MSK_ARM_LEAD'
  and s.OSUSER='yakushev'
  and s.MACHINE='PRM-WS-0006'



C:\Distr\ojdbc-full\OJDBC-Full

https://mkyong.com/spring-boot/spring-boot-jdbc-oracle-database-commons-dbcp2-example/

ucp
https://docs.oracle.com/database/121/JJUCP/get_started.htm#JJUCP8120
https://stackoverflow.com/questions/1427890/oracledatasource-vs-oracle-ucp-pooldatasource
https://stackoverflow.com/questions/31004828/difference-between-oracledatasource-oracle-ucp-commons-dbcp-and-tomcat-connec

ons
https://docs.oracle.com/database/121/RACAD/GUID-E242A789-7118-45CD-82C8-2CB52248C47F.htm
https://www.oracle.com/database/technologies/universal-connection-pool-faq.html#ons

https://www.oracle.com/database/technologies/jdbcdriver-ucp-downloads.html

https://www.oracle.com/database/technologies/universal-connection-pool-faq.html#ons
https://docs.oracle.com/database/121/RACAD/GUID-E242A789-7118-45CD-82C8-2CB52248C47F.htm

https://docs.oracle.com/database/121/RACAD/GUID-5396E213-81D5-45AA-84C0-FA74D9CD24A4.htm

You can enable FCF for Universal Connection Pool or Implicit Connection Cache. Oracle recommends using the Universal Connection Pool for Java because the Implicit Connection Cache is deprecated.


LRU cache
https://scalac.io/how-to-write-a-completely-lock-free-concurrent-lru-cache-with-zio-stm/