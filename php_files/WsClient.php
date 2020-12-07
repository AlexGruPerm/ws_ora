<?php

namespace App\Helpers;
use Debugbar;

class WsClient {
    protected string $ws_url;
    protected string $user_session;
    protected int $cont_encoding_gzip_enabled;
    protected int $request_timeout_ms;

public function __construct(string $ws_url,
                            string $user_session,
                            int    $cont_encoding_gzip_enabled = 0,
                            int    $request_timeout_ms = 5000) {
    $this->ws_url = $ws_url;
    $this->user_session = $user_session;
    $this->cont_encoding_gzip_enabled = $cont_encoding_gzip_enabled;
    $this->request_timeout_ms = $request_timeout_ms;
}

 private function HTTPPost(array $params) {
        $postVariables = json_encode($params);
        //Debugbar::addMessage(json_encode($postVariables));
        $headers = array();
        $headers[] = 'Accept: application/json';
        if ($this->cont_encoding_gzip_enabled == 1){
          $headers[] = 'Accept-Encoding: gzip';
        }
        $headers[] = 'Content-Length:'.strlen($postVariables);
        $headers[] = 'Content-Type: application/json; charset=utf-8';
        $headers[] = 'Connection: keep-alive';
        $headers[] = 'Pragma: no-cache';
        $headers[] = 'Cache-Control: no-cache';
        $ch    = curl_init();
        curl_setopt($ch, CURLOPT_URL, $this->ws_url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postVariables);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        //tell cURL to decode the response automatically (gzip)
        if ($this->cont_encoding_gzip_enabled == 1){
          Debugbar::addMessage('WE ARE HERE - cont_encoding_gzip_enabled =1');
          curl_setopt($ch, CURLOPT_ENCODING, "");
        }
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);
        curl_setopt($ch, CURLOPT_HEADER, 0);// get response header or not.
        $response = curl_exec($ch);
        /* If you uncomment this, set CURLOPT_HEADER to 1.
        // Retudn headers seperatly from the Response Body
        // to take headers set CURLOPT_HEADER=1
        $header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
        $headers = substr($response, 0, $header_size);
        $headers = explode("\r\n", $headers);
        */
        /*
        foreach ($headers as &$value) {
          Debugbar::addMessage('HEADER:'.$value);
        }
        */
        curl_close($ch);

        $resJson = json_decode($response);
        return $resJson;
    }



    /**
    */
    public function getData($query_json) {
     $time_start = microtime(true);
     $list_requested_queries = array();
     array_push($list_requested_queries, $query_json);

     $reqParamArray = array(
        'user_session' => $this->user_session,
        'cont_encoding_gzip_enabled' => $this->cont_encoding_gzip_enabled,
        'thread_pool' => 'block',
        'request_timeout_ms' => $this->request_timeout_ms,
        'queries' => $list_requested_queries);

     $wsRespData = $this->HTTPPost($reqParamArray);
     $afterHttp = microtime(true);

     if ($wsRespData == null){
      throw new \Exception("Web service is not available.");
     }

     $res = array();
     if ($wsRespData->status == "ok"){
      $res["status"] = true;
      $res["message"] = "";
      $resDatArr = array();
        foreach($wsRespData->data->dicts[0]->rows as $key => $value) {
        $resDatArr[] = (array)$value;
        }
      $res["data"] = $resDatArr;
     } elseif ($wsRespData->status == "db error") {
      $res["status"] = false;
      $res["message"] = $wsRespData->message;
     }
     return $res;
    }

}
