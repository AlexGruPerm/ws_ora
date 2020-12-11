<?php

namespace App\Helpers;
use Debugbar;

class WsClient {
    protected string $ws_url;
    protected string $user_session;
    protected int    $cont_encoding_gzip_enabled;
    protected int    $request_timeout_ms;

    public function __construct(string $ws_url,
                                string $user_session,
                                int    $cont_encoding_gzip_enabled = 0,
                                int    $request_timeout_ms = 5000) {
        $this->ws_url = $ws_url;
        $this->user_session = $user_session;
        $this->cont_encoding_gzip_enabled = $cont_encoding_gzip_enabled;
        $this->request_timeout_ms = $request_timeout_ms;
    }

    protected static $_instance = null;

    public static function instance(): self
        {
            if (static::$_instance === null) {
                static::$_instance = new static(
                        "http://127.0.0.1:8080/data",
                        "c4ec52189bd51acb95bc2a5082c7c014");
            }

            return static::$_instance;
        }

 private function check_alive($url, $timeout = 50) {
        $ch = curl_init($url);

        // Set request options
        curl_setopt_array($ch, array(
          CURLOPT_FOLLOWLOCATION => true,
          CURLOPT_NOBODY => true,
          CURLOPT_TIMEOUT_MS => $timeout,
          CURLOPT_USERAGENT => "page-check/1.0"
        ));

        // Execute request
        curl_exec($ch);

        // Check if an error occurred
        if(curl_errno($ch)) {
          curl_close($ch);
          return false;
        }

        // Get HTTP response code
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        // Page is alive if 200 OK is received
        return $code === 200;
      }

 private function HTTPPost(array $params) {

        if (!$this->check_alive($this->ws_url))
         return null;

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
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 50);
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
    public function getData($query_json, $query_context = null) {
        $time = microtime(true);
        $list_requested_queries = array();
        array_push($list_requested_queries, $query_json);

        $reqParamArray = array(
           'user_session' => $this->user_session,
           'cont_encoding_gzip_enabled' => $this->cont_encoding_gzip_enabled,
           'thread_pool' => 'block',
           'request_timeout_ms' => $this->request_timeout_ms,
           'queries' => $list_requested_queries);

        if ($query_context !== null) {
            $reqParamArray["context"]=$query_context;
        }

        $wsRespData = $this->HTTPPost($reqParamArray);

        if ($wsRespData == null){
         throw new \Exception("Web service is not available. Check time = ".
                 number_format(microtime(true) - $time, 4, '.', ',') . ' sec');
        }

        $res = array();
        if ($wsRespData->status == "ok"){
         $res["status"] = true;
         $res["message"] = "";
         $res["src_time"] = $wsRespData->data->dicts[0]->src." ".
                 $wsRespData->data->dicts[0]->time." sec.";
         $resDatArr = array();
           foreach($wsRespData->data->dicts[0]->rows as $key => $value) {
           $resDatArr[] = (array)$value;
           }
         $res["data"] = $resDatArr;
        } elseif ($wsRespData->status == "db error") {
         $res["status"] = false;
         $res["message"] = $wsRespData->message;
        }

           Debugbar::addMessage([
               'source' => 'web service',
               'result' => $res,
               'status' => $res['status'],
               'time' =>  number_format(microtime(true) - $time, 4, '.', ',') . ' sec'
           ], $query_json["query"]);

        return $res;
    }

}
