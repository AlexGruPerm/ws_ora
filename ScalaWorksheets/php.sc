<?php

namespace App\Helpers;
use Debugbar;

class WsClient {

private $srv;

public function __construct() {
$this->$srv = new Service();
}

private function HTTPPost($url, array $params) {
$postVariables = json_encode($params);
$headers = array();
$headers[] = 'Accept: application/json';
//$headers[] = 'Accept-Encoding: gzip';
$headers[] = 'Content-Length:'.strlen($postVariables);
$headers[] = 'Content-Type: application/json; charset=utf-8';
$headers[] = 'Connection: keep-alive';
$headers[] = 'Pragma: no-cache';
$headers[] = 'Cache-Control: no-cache';

$ch    = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $postVariables);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
//tell cURL to decode the response automatically (gzip)
//curl_setopt($ch, CURLOPT_ENCODING, "");
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);
curl_setopt($ch, CURLOPT_HEADER, 0);// get response header or not.
$response = curl_exec($ch);

/* If you uncomment this, set CURLOPT_HEADER to 1.
// Retudn headers seperatly from the Response Body
// to take headers set CURLOPT_HEADER=1
$header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
$headers = substr($response, 0, $header_size);
$headers = explode("\r\n", $headers);
foreach ($headers as &$value) {
Service::trace($value, 'WS RESP HEADER');
}
*/

curl_close($ch);

//json_decode only works with UTF-8.
$resJson = json_decode($response); //$resJson is object, not an array
$resultStatus = $resJson->status;
$resultDicts = $resJson->data->dicts;

Service::trace(strtoupper($resultStatus), $common_debug.'HTTP Post status');

if (isset($resultStatus) and $resultStatus == 'ok'){
return $resultDicts;
} else {
//todo: add here correct error handler.
throw new Exception("Service not available.".
" Error: error message.");
}
}


/**
* Convert row from json to array.
* Input: $row:
* [{"name" : "id",        "value" : "2"},
{"name" : "parent_id", "value" : "0"},
{"name" : "name",      "value" : "ЗП - Наука"}]
*
* Output:
(("id" => "2"),("parent_id" => "0"),("name" => "ЗП - Наука"))
*/
private function rowToArray($row) {
$singleRow = array();
foreach($row as $cell){
$singleRow[$cell->name] = $cell->value;
}
return $singleRow;
}

/**
*
*/
private function getDataFieldName($thisDictName){
return strtolower(Header::$sql_dict[$thisDictName]['field']);
}

private function getDataFullFieldName($thisDictName){
return strtolower(Header::$sql_dict[$thisDictName]['fullField']);
}

private function getDataShortFieldName($thisDictName){
return strtolower(Header::$sql_dict[$thisDictName]['shortField']);
}

/**
*/
private function parseResponseData(&$resultDicts,
$combCode,
$short,
$showInn) {
$combinedRes = array();
/*
* $combCode:
*  oiv       => ID_COL_1
*  territory => ID_COL_2
*  okfs      => ID_COL_3
*/

foreach($resultDicts as $DictKey => $value) {
$thisDictName = $resultDicts[$DictKey]->name;
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
if ($thisDictName == 'industry_class'/*'edict_cat'*/) {
$dataFieldName = $this->getDataFullFieldName($thisDictName);
$tmpRows = array();
foreach($resultDicts[$DictKey]->rows as $row){
$singleRow = $this->rowToArray($row);
$tmpRows[$singleRow['id'].'_'.$combCode[$thisDictName]] = iconv("UTF-8", "CP1251",
$singleRow[$dataFieldName]
);
}
$tmpRows[-1] = 'Итого';
$combinedRes[$combCode[$resultDicts[$DictKey]->name]] = $tmpRows;
}
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
elseif ($thisDictName == 'institution' && $showInn) {
$dataFieldName = $this->getDataFieldName($thisDictName);
$tmpRows = array();
foreach($resultDicts[$DictKey]->rows as $row){
$singleRow = $this->rowToArray($row);
$tmpRows[$singleRow['id'].'_'.$combCode[$thisDictName]] = iconv("UTF-8", "CP1251",
$singleRow[$dataFieldName].' ('.$singleRow['institution_inn'].')'
);
}
$tmpRows[-1] = 'Итого';
$combinedRes[$combCode[$resultDicts[$DictKey]->name]]= $tmpRows;
}
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
elseif ($short) {
$dataFieldName = $this->getDataShortFieldName($thisDictName);
$tmpRows = array();
foreach($resultDicts[$DictKey]->rows as $row){
$singleRow = $this->rowToArray($row);
$tmpRows[$singleRow['id'].'_'.$combCode[$thisDictName]] = iconv("UTF-8", "CP1251",
$singleRow[$dataFieldName]
);
}
$tmpRows[-1] = 'Итого';
$combinedRes[$combCode[$resultDicts[$DictKey]->name]] = $tmpRows;
}
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
else {
$dataFieldName = $this->getDataFieldName($thisDictName);
$tmpRows = array();
foreach($resultDicts[$DictKey]->rows as $row){
$singleRow = $this->rowToArray($row);
$tmpRows[$singleRow['id'].'_'.$combCode[$thisDictName]] = iconv("UTF-8", "CP1251",
$singleRow[$dataFieldName]
);
}
$tmpRows[-1] = 'Итого';
$combinedRes[$combCode[$resultDicts[$DictKey]->name]] = $tmpRows;
}

}

return $combinedRes;
}

/**
* Process input array with dictionaries meta information
* and request web service that execute db queries in parallel.
*
* @param $comb fixed dimensions of report table:
* ID_COL_1; ID_COL_2; ID_COL_3
*
* @param $left_header instance of class Header
*     {ID_COL_1;0;1;ID_COL_1;tratra;text;krai_oiv;Краевые ГРБС},
*     {ID_COL_2;0;1;ID_COL_2;tratra;text;industry_class;Отрасль/Подотрасль}
*
* @return array of dictionaries, like :
*
*/
public function getData($wsurl, $comb, $left_headers, $short, $showInn) {
$time_start = microtime(true);
Service::trace("Begin load dicts with web service.",
$common_debug."loadDicts ".$common_postfix);
/* Array of arrays,
* array (
array(
"name" => "oiv",
"db" => "db2_msk_gp",
"proc" => "prm_salary.pkg_web_cons_rep_form_type_list(refcur => ?)"
),
array(
"name" => "okfs",
"db" => "db2_msk_gp",
"proc" => "prm_salary.pkg_web_cons_rep_okved_list(refcur => ?)"
)
)
*/
$list_requested_dicts = array();
foreach($comb as $combCode){
$thisDict = array();
$thisCombCode = array();
$thisDictCode = $left_headers[$combCode]['CODE'];
$data = Header::$sql_dict[$thisDictCode];
$thisDict['name'] = $thisDictCode;
$thisDict['db'] = 'dbp';
$thisDict['proc'] = $data['sql'].'(refcur => ?'
.($data['bind'] ? ', p_user_id => '.User::$id : '').')';
if ($thisDictCode == 'okfs') {
$thisDict['reftables'] = Array('sl_d_okfs');
}
array_push($list_requested_dicts, $thisDict);
$combCodeArray[$thisDictCode] = $combCode;
// [{k=territory,v=ID_COL_1}, {k=oiv,v=ID_COL_2}]
}

$reqDictsArray = array(
'user_session' => Auth::$session,
'cont_encoding_gzip_enabled' => 0,
'thread_pool' => 'block',
'request_timeout_ms' => 5000,
'cache_live_time' => 60000,
'dicts' => $list_requested_dicts);

$wsRespData = $this->HTTPPost($wsurl, $reqDictsArray);
$afterHttp = microtime(true);
Service::trace(($afterHttp - $time_start), 'ws [HTTPPost] Duration sec.');

$wsResDicts = $this->parseResponseData($wsRespData, $combCodeArray,
$short, $showInn);

$afterParsing = microtime(true);
Service::trace(($afterParsing - $afterHttp),
'ws [ResponseParsing] Duration sec.');

foreach($wsRespData as $DictKey => $value) {
$thisDictName       = $wsRespData[$DictKey]->name;
$thisDictConnDurMs  = $wsRespData[$DictKey]->connDurMs;
$thisDictExecDurMs  = $wsRespData[$DictKey]->execDurMs;
$thisDictFetchDurMs = $wsRespData[$DictKey]->fetchDurMs;
Service::trace(
$thisDictName." conn = $thisDictConnDurMs exec = $thisDictExecDurMs fetch = $thisDictFetchDurMs "
,'Timings');
}

Service::trace("End function ",
$common_debug."loadDicts ".$common_postfix);

//throw new Exception('Service not available. Error : error message.');
return $wsResDicts;
}


}


