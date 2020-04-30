package testsjsons

object CollectJsons {

  val reqJsonOra1 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "cache_live_time" : 60000,
      |                  "context" : "begin
      |                                    MSK_ARM_LEAD.PKG_ARM_STRUCT.set_context_variables(
      |                                       p_user_id =>  37317,
      |                                       p_user_role =>  3,
      |                                       p_budget =>  5,
      |                                       p_ddate => 20190701,
      |                                       p_appg_date =>  20180701,
      |                                       p_book =>  9,
      |                                       p_tab =>  5,
      |                                       p_slice =>  300,
      |                                       p_pok =>  null);
      |                               end;
      |                              ",
      |                  "queries": [
      |                                {
      |                                 "name" : "arm_data",
      |                                 "qt" : "func_cursor",
      |                                 "query" : "msk_arm_lead.pkg_arm_data.f_get_data",
      |                                 "reftables" : ["msk_arm_lead.t_keys","msk_arm_lead.t_data"]
      |                               }
      |                   ]}
      """.stripMargin

  // 20190701,
  //select msk_arm_lead.pkg_arm_data.f_get_data from dual
  //       msk_arm_lead.pkg_econom.f_get_data

  /*
                                  {
                                 "name" : "query1",
                                 "qt" : "func",
                                 "query" : "select msk_arm_lead.pkg_econom.f_get_data from dual",
                                 "reftables" : ["msk_arm_lead.econom_indicator_tab","msk_arm_lead.econom_data"]
                               },
*/

/*
                                {
                                 "name" : "s1",
                                 "qt" : "select",
                                 "query" : "select * from t_data td where td.ddate=20190701 and id_pok=2443",
                                 "reftables" : ["msk_arm_lead.t_data"]
                               }
  */

  val reqJsonOra2 =
    """
      |               {  "user_session" : "c4ec52189bd51acb95bc2a5082c7c014",
      |                  "cont_encoding_gzip_enabled" : 1,
      |                  "thread_pool" : "block",
      |                  "request_timeout_ms": 5000,
      |                  "cache_live_time" : 60000,
      |                  "context" : "begin
      |                                    MSK_ARM_LEAD.PKG_ARM_STRUCT.set_context_variables(
      |                                       p_user_id =>  37317,
      |                                       p_user_role =>  1,
      |                                       p_budget =>  15,
      |                                       p_ddate =>  20190701,
      |                                       p_appg_date =>  null,
      |                                       p_book =>  null,
      |                                       p_tab =>  null,
      |                                       p_slice =>  null,
      |                                       p_pok =>  null);
      |                               end;
      |                              ",
      |                  "queries": [
      |                                {
      |                                 "name" : "query1",
      |                                 "qt" : "func",
      |                                 "query" : "select msk_arm_lead.pkg_econom.f_get_data from dual",
      |                                 "reftables" : ["msk_arm_lead.econom_indicator_tab","msk_arm_lead.econom_data"]
      |                               },
      |                                {
      |                                 "name" : "query2",
      |                                 "qt" : "proc",
      |                                 "query" : "prm_salary.pkg_web_posit_rep_position_group_list(refcur => ?)",
      |                                 "reftables" : ["msk_arm_lead.econom_data","msk_arm_lead.econom_data_source"]
      |                                },
      |                                {
      |                                 "name" : "query3",
      |                                 "qt" : "select",
      |                                 "query" : "select * from msk_arm_lead.econom_data",
      |                                 "reftables" : ["msk_arm_lead.econom_data","msk_arm_lead.econom_data_source"]
      |                                },
      |                                {
      |                                 "name" : "trash",
      |                                 "query" : "select * from msk_arm_lead.econom_data",
      |                                 "reftables" : ["msk_arm_lead.econom_data","msk_arm_lead.econom_data_source"]
      |                                }
      |                   ]}
      """.stripMargin

}
