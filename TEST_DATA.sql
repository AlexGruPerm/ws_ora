clear;

drop table dat;
drop table d_dict_terr;
drop table d_dict_oiv;
drop table d_dict_indic;
drop table d_dict_prop;

create table d_dict_terr(
 id   integer constraint pk_d_dict_terr primary key,
 name varchar2(64)
);

insert into d_dict_terr
select rownum as id,
       initcap(dbms_random.string(len => greatest(5,round(dbms_random.value*20)),opt => 'x'))
  from dual
connect by rownum<=100;

create table d_dict_oiv(
 id   integer constraint pk_d_dict_oiv primary key,
 name varchar2(64)
);
insert into d_dict_oiv
select rownum as id,
       initcap(dbms_random.string(len => greatest(5,round(dbms_random.value*20)),opt => 'x'))
  from dual
connect by rownum<=20;

create table d_dict_indic(
 id   integer constraint pk_d_dict_indic primary key,
 name varchar2(64)
);
insert into d_dict_indic
select rownum as id,
       initcap(dbms_random.string(len => greatest(5,round(dbms_random.value*20)),opt => 'x'))
  from dual
connect by rownum<=1000;

create table d_dict_prop(
 id   integer constraint pk_d_dict_prop primary key,
 name varchar2(64)
);
insert into d_dict_prop
select rownum as id,
       initcap(dbms_random.string(len => greatest(5,round(dbms_random.value*20)),opt => 'x'))
  from dual
connect by rownum<=100;

create table dat(
ddate date not null,
d_dict_terr_id integer not null constraint propk_dat_d_dict_terr references d_dict_terr(id),
d_dict_oiv_id integer not null constraint propk_dat_d_dict_oiv references d_dict_oiv(id),
d_dict_indic_id integer not null constraint propk_dat_d_dict_indic references d_dict_indic(id),
d_dict_prop_id integer not null constraint propk_dat_d_dict_prop references d_dict_prop(id),
val number,
constraint uk_dat unique(ddate,d_dict_terr_id,d_dict_oiv_id,d_dict_indic_id,d_dict_prop_id)
);

purge recyclebin;

insert into dat
select distinct
       ddate,
       t_id,
       ov_id,
       ind_id,
       prop_id,
       round(dbms_random.value*1000) as val
from(
select distinct
       to_date('01.01.2017','dd.mm.yyyy') + round(dbms_random.value*1000) as ddate,
       t.id as t_id,
       ov.id as ov_id,
       ind.id as ind_id,
       prop.id as prop_id
  from dual,
       d_dict_terr t,
       d_dict_oiv  ov,
       d_dict_indic ind,
       d_dict_prop prop
where rownum <= 1000000
order by dbms_random.value;

commit;