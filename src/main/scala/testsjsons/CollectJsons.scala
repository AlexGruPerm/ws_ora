package testsjsons

object CollectJsons {

  val reqJsonOra1 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "queries": [
      |                                {
      |                                 "name" : "select1",
      |                                 "nocache" : 0,
      |                                 "qt" : "select",
      |                                 "convtype" : "str",
      |                                 "query" : "select * from OLAP_DATA_GR  where rownum <= 8000"
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1_ =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "context" : "begin
      |                                    MSK_ARM_LEAD.PKG_ARM_STRUCT.SET_CONTEXT_VARIABLES(
      |                                           p_user_id =>  37317,
      |                                           p_user_role =>  1,
      |                                           p_budget =>  15,
      |                                           p_ddate =>  20201001,
      |                                           p_appg_date =>  20191001,
      |                                           p_book =>  9,
      |                                           p_tab =>  5,
      |                                           p_slice =>  300,
      |                                           p_pok =>  null
      |                                       );
      |                               end;
      |                              ",
      |                  "queries": [
      |                                {
      |                                 "name" : "func_priv_1",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_user_admin.check_user_has_priv(37317,'problem_level_mayor')",
      |                                 "reftables" : [
      |                                                "msk_admin.t_roles",
      |                                                "msk_admin.t_privs",
      |                                                "msk_admin.t_users_privs",
      |                                                "msk_admin.t_users_roles",
      |                                                "msk_admin.t_users_privs"
      |                                                ]
      |                               },
      |                                {
      |                                 "name" : "func_priv_2",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_user_admin.check_user_has_priv(37317,'problem_level_deputy')",
      |                                 "reftables" : [
      |                                                "msk_admin.t_roles",
      |                                                "msk_admin.t_privs",
      |                                                "msk_admin.t_users_privs",
      |                                                "msk_admin.t_users_roles",
      |                                                "msk_admin.t_users_privs"
      |                                                ]
      |                               },
      |                                {
      |                                 "name" : "func_priv_3",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_user_admin.check_user_has_priv(37317,'see_all')",
      |                                 "reftables" : [
      |                                                "msk_admin.t_roles",
      |                                                "msk_admin.t_privs",
      |                                                "msk_admin.t_users_privs",
      |                                                "msk_admin.t_users_roles",
      |                                                "msk_admin.t_users_privs"
      |                                                ]
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1_prv_1 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "queries": [
      |                                {
      |                                 "name" : "func_get_dashboard",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_widget.get_dashboard(37317)",
      |                                 "reftables" : [
      |                                                "msk_admin.t_users_privs",
      |                                                "msk_admin.t_privs",
      |                                                "msk_admin.t_users_roles",
      |                                                "msk_admin.t_roles_privs",
      |                                                "msk_admin.t_privs",
      |                                                "msk_arm_lead.widget_dashboard",
      |                                                "msk_arm_lead.widget"
      |                                                ]
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1____ =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "queries": [
      |                                {
      |                                 "name" : "func_get_types",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "WSORA.pkg_test.func_check_types",
      |                                 "reftables" : []
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1_wt =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "queries": [
      |                                {
      |                                 "name" : "func_get_widget_type",
      |                                 "nocache" : 0,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_widget.get_widget_type",
      |                                 "reftables" : ["msk_arm_lead.widget_meta_property","msk_arm_lead.widget_type"]
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1_last =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
      |                  "context" : "begin
      |                                    wsora.pkg_test.set_global_context(
      |                                           param1 =>  37317,
      |                                           param2 =>  to_date('10.05.2020','dd.mm.yyyy')
      |                                       );
      |                               end;
      |                              ",
      |                  "queries": [
      |                                {
      |                                 "name" : "func_get_cursor_err1",
      |                                 "nocache" : 1,
      |                                 "qt" : "func_cursor",
      |                                 "query" : "wsora.pkg_test.func_get_cursor_err1",
      |                                 "reftables" : ["wsora.dat"]
      |                               }
      |                   ]}
      """.stripMargin

  val reqJsonOra1_012 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "nocache" : 0,
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
      |                                 "nocache" : 1,
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
      |                                 "query" : "wsora.pkg_test.proc_v1(?)",
      |                                 "reftables" : ["wsora.dat"]
      |                                },
      |                                {
      |                                 "name" : "proc_v11",
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
      |                                 "name" : "proc_v3",
      |                                 "qt" : "proc_cursor",
      |                                 "query" : "wsora.pkg_test.proc_v2(?, p_id_oiv => 1)",
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


  val reqJsonOra1_0101 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 8000,
      |                  "nocache" : 0,
      |                  "queries": [
      |{
      |                                 "name" : "func_simple_1",
      |                                 "qt" : "func_simple",
      |                                 "query" : "wsora.pkg_test.func_simple",
      |                                 "reftables" : ["wsora.D_DICT_OIV","wsora.D_DICT_TERR","wsora.DAT"]
      |                               }
      |                   ]}
      |""".stripMargin

  val reqJsonOra1_33 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 8000,
      |                  "nocache" : 0,
      |                  "queries": [
      |{
      |                                 "name" : "1",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "2",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "3",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "4",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "5",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "6",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "7",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "8",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "9",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "10",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "11",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "12",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "13",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "14",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "15",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 20) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "16",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "17",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "18",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "19",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "20",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "21",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "22",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "23",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "24",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "25",
      |                                 "qt" : "select",
      |                                 "query" : "select pkg_test.func_wait(slp => 10) from dual",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               }
      |                   ]}
      |""".stripMargin

  val reqJsonOra1_0 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "sync",
      |                  "request_timeout_ms": 8000,
      |                  "nocache" : 0,
      |                  "queries": [
      |{
      |                                 "name" : "w682",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 682)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w729",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 729)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w730",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 730)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w731",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 731)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w732",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 732)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w735",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 735)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w740",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 740)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w741",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 741)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w743",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 743)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w775",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 775)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w788",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 788)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w811",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 811)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w816",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 816)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               },
      |{
      |                                 "name" : "w820",
      |                                 "qt" : "func_cursor",
      |                                 "query" : " msk_arm_lead.pkg_widget.get_widget_data(p_user_id => 37317, p_widget_id => 820)",
      |                                 "reftables" : ["msk_arm_lead.v_contract_widget_data"]
      |                               }
      |                   ]}
      |""".stripMargin

  val reqJsonOra1__ =
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