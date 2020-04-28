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
      |                                       p_user_role =>  1,
      |                                       p_budget =>  15,
      |                                       p_ddate =>  20200415,
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
      |                               }
      |                   ]}
      """.stripMargin

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
      |                                       p_ddate =>  20200415,
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
