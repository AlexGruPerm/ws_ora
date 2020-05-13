package testsjsons

object CollectJsons {

  val reqJsonOra1_full =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "cache_live_time" : 60000,
      |                  "context" : "begin
      |                                    wsora.pkg_test.set_global_context(
      |                                           param1 =>  37317,
      |                                           param2 =>  to_date('10.05.2020','dd.mm.yyyy')
      |                                       );
      |                               end;
      |                              ",
      |                  "queries": [
      |                                {
      |                                 "name" : "func_get_cursor_v1",
      |                                 "qt" : "func_cursor",
      |                                 "query" : "wsora.pkg_test.func_get_cursor_v1",
      |                                 "reftables" : ["wsora.dat"]
      |                               },
      |                                {
      |                                 "name" : "func_get_cursor_v2",
      |                                 "qt" : "func_cursor",
      |                                 "query" : "wsora.pkg_test.func_get_cursor_v2(to_date('01.01.2018','dd.mm.yyyy'))",
      |                                 "reftables" : ["wsora.dat"]
      |                               },
      |                                {
      |                                 "name" : "func_get_cursor_v2_p",
      |                                 "qt" : "func_cursor",
      |                                 "query" : "wsora.pkg_test.func_get_cursor_v2(d_date => to_date('01.01.2018','dd.mm.yyyy'))",
      |                                 "reftables" : ["wsora.dat"]
      |                               },
      |                                {
      |                                 "name" : "proc_v1",
      |                                 "qt" : "proc_cursor",
      |                                 "query" : "wsora.pkg_test.proc_v1(rc)",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "proc_v1",
      |                                 "qt" : "proc_cursor",
      |                                 "query" : "wsora.pkg_test.proc_v1(refcur => ?)",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "proc_v2",
      |                                 "qt" : "proc_cursor",
      |                                 "query" : "wsora.pkg_test.proc_v2(refcur => ?, p_id_oiv => 1)",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "func_simple_1",
      |                                 "qt" : "func_simple",
      |                                 "query" : "wsora.pkg_test.func_simple",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "func_simple_2",
      |                                 "qt" : "func_simple",
      |                                 "query" : "wsora.pkg_test.func_simple_param(p_param_id => 1)",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "select_1",
      |                                 "qt" : "select",
      |                                 "query" : "select t.* from wsora.D_DICT_OIV t",
      |                                 "reftables" : ["wsora.D_DICT_OIV"]
      |                                },
      |                                {
      |                                 "name" : "select_2",
      |                                 "qt" : "select",
      |                                 "query" : "select t.id from wsora.D_DICT_OIV t where t.id<=6",
      |                                 "reftables" : ["wsora.D_DICT_OIV"]
      |                                }
      |                   ]}
      """.stripMargin

  val reqJsonOra1 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "cache_live_time" : 60000,
      |                  "queries": [
      |                                {
      |                                 "name" : "select_2",
      |                                 "qt" : "select",
      |                                 "query" : "select t.id from wsora.D_DICT_OIV t where t.id<=6",
      |                                 "reftables" : ["wsora.D_DICT_OIV"]
      |                                }
      |                   ]}
      """.stripMargin




}