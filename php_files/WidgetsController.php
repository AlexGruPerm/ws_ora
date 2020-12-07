<?php

namespace App\Http\Controllers;

use App\Helpers\DB;
use App\Helpers\WsClient;
use App\Services\DBService;
use http\Env\Response;
use Illuminate\Http\JsonResponse;
use PDO;
use stdClass;
use Debugbar;
use \Illuminate\Support\Collection;

class WidgetsController extends Controller
{

    /*
    //http://json.parser.online.fr/

    */
    public function initialState()
    {   // Old code:
        /*
        $types = DB::fetchLower('msk_arm_lead.pkg_widget.get_widget_type');

        return response()->json([
            'types' => $types['data'],
            'widgets' => $this->widgets()->getOriginalContent()
        ], $types['status'] ? 200 : 400);
        */


        $ws = new WsClient("http://127.0.0.1:8080/data",
                           "c4ec52189bd51acb95bc2a5082c7c014");
        $query = [
                   "name" => "func_get_widget_type",
                   "nocache" => 0,
                   "qt" => "func_cursor",
                   "query" => "msk_arm_lead.pkg_widget.get_widget_type",
                   "reftables" => ["msk_arm_lead.widget_meta_property",
                                   "msk_arm_lead.widget_type"]
                 ];

        try {
            $time = microtime(true);
            $types = $ws->getData($query);
            Debugbar::addMessage([
                'source' => 'web service',
                'result' => $types,
                'time' =>  number_format(microtime(true) - $time, 4, '.', ',') . ' sec'
            ], $query["query"]);

        } catch(\Exception $wsExc){
            Debugbar::error($wsExc->getMessage());
            $types = DB::fetchLower('msk_arm_lead.pkg_widget.get_widget_type');
            Debugbar::addMessage('Retrieving data from the database.');
        }

        return response()->json([
            'types' => $types['data'],
            'widgets' => $this->widgets()->getOriginalContent()
        ], ($types['status'] or $types['status'] == "ok") ? 200 : 400);

    }


    public function widgets()
    {   // Old code:
        /*
        $widgets = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_dashboard', [
            'p_user_id' => auth()->id()
        ], PDO::PARAM_STMT);
        */
        //Debugbar::addMessage(json_encode($widgets),'$widgets');


        $ws = new WsClient("http://127.0.0.1:8080/data",
                           "c4ec52189bd51acb95bc2a5082c7c014");
        //todo: check it - "msk_arm_lead.pkg_widget.get_dashboard({$auth()->id()})"
        $query = [
                    "name" => "func_get_dashboard_2",
                    "nocache" => 0,
                    "qt" => "func_cursor",
                    "query" => "msk_arm_lead.pkg_widget.get_dashboard(".auth()->id().")",
                    "reftables" => [
                                   "msk_admin.t_users_privs",
                                   "msk_admin.t_privs",
                                   "msk_admin.t_users_roles",
                                   "msk_admin.t_roles_privs",
                                   "msk_admin.t_privs",
                                   "msk_arm_lead.widget_dashboard",
                                   "msk_arm_lead.widget"
                                   ]
                 ];

        try {
            $time = microtime(true);
            $widgets_data = $ws->getData($query)["data"];
            $widgets = collect($widgets_data);
            Debugbar::addMessage([
                'source' => 'web service',
                'result' => $widgets,
                'time' =>  number_format(microtime(true) - $time, 4, '.', ',') . ' sec'
            ], $query["query"]);
        } catch(\Exception $wsExc){
            Debugbar::error($wsExc->getMessage());
            $widgets = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_dashboard', [
            'p_user_id' => auth()->id()
            ], PDO::PARAM_STMT);
            Debugbar::addMessage([
                'source' => 'db',
                'result' => $widgets,
                'time' =>  number_format(microtime(true) - $time, 4, '.', ',') . ' sec'
            ], $query["query"]);
            Debugbar::addMessage('Retrieving data from the database.');
        }


        // Old code:
        /*
        $widgetsData = $widgets->filter->is_widget->mapWithKeys(function ($widget) {
            $data = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_widget_data', [
                'p_user_id' => auth()->id(),
                'p_widget_id' => $widget['widget_id']
            ], PDO::PARAM_STMT);

            return [$widget['widget_id'] => $data];
        });
        */



        $widgetsData = $widgets->filter->is_widget->mapWithKeys(function ($widget) {
            try{
            $ws = new WsClient("http://127.0.0.1:8080/data",
                               "c4ec52189bd51acb95bc2a5082c7c014");
            $query = [
                        "name" => "func_get_dashboard_2",
                        "nocache" => 0,
                        "qt" => "func_cursor",
                        "query" => "msk_arm_lead.pkg_widget.get_widget_data(".auth()->id().",".$widget['widget_id'].")",
                        "reftables" => [
                                        "msk_admin.widget",
                                        "msk_admin.widget_type",
                                        "msk_admin.v_contract_widget_data",
                                        "msk_admin.widget_contract_meta",
                                        "msk_admin.widget_params",
                                        "msk_admin.v_arm_exec_indic_tree",
                                        "msk_admin.v_arm_exec_indic_period_data",
                                        "msk_admin.v_arm_exec_np_fin_data",
                                        "msk_admin.v_arm_rp_name_order_ind",
                                        "msk_admin.v_arm_exec_total_ind_res_count",
                                        "msk_admin.v_arm_national_project",
                                        "msk_admin.v_arm_regional_project ",
                                        "msk_admin.widget_np_color",
                                        "msk_admin.widget_rp_color",
                                        "msk_admin.widget_hidden_np",
                                        "msk_admin.v_arm_rp_name_order_ind"
                                       ]
                 ];
            $time = microtime(true);
            $widgets_data = $ws->getData($query)["data"];
            $data = collect($widgets_data);
            Debugbar::addMessage([
                'source' => 'web service',
                'result' => $data,
                'time' =>  number_format(microtime(true) - $time, 4, '.', ',') . ' sec'
            ], $query["query"]);
            }catch(\Exception $wsExc){
                Debugbar::error($wsExc->getMessage());
                $data = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_widget_data', [
                    'p_user_id' => auth()->id(),
                    'p_widget_id' => $widget['widget_id']
                ], PDO::PARAM_STMT);
                 Debugbar::addMessage('Retrieving data from the database.');
            }

            return [$widget['widget_id'] => $data];
        });

        return response()->json([
            'items' => $widgets,
            'widgetsData' => $widgetsData
        ]);
    }

    public function settings()
    {
        $settings = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_widget_type_property', [
            'p_widget_type_id' => request('type')
        ], PDO::PARAM_STMT);

        $settings->transform(function ($item) {
            $item['ref_from'] = $item['ref_from'] ? explode(',', $item['ref_from']) : [];
            return $item;
        });

        $directories = $settings->filter(function ($item) {
            return empty($item['ref_from']);
        })->mapWithKeys(function ($item) {
            return [
                $item['property_code'] => $this->directory($item['property_code'], $item['property_id'])->getOriginalContent()
            ];
        });

        $values = with(request('widget'), function ($widgetId) use ($settings) {
            return $settings->mapWithKeys(function ($setting) use ($widgetId) {
                $value = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_widget_prop_values', [
                    'p_widget_id' => $widgetId,
                    'p_prop_name' => $setting['property_code']
                ], PDO::PARAM_STMT)->pluck('prop_value');

                return [$setting['property_code'] => $setting['property_type'] === 'single' ? $value->first() : $value->all()];
            });
        });

        return response()->json([
            'settings' => $settings,
            'directories' => $directories,
            'values' => $values ?? new stdClass()
        ]);
    }

    public function directory($code, $propertyId = null)
    {
        $directory = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_dict', [
            'p_property_id' => $propertyId ?? request('property'),
            'c_ref_from' => DBService::paramCollectionOfStrings(request('refs')),
            'c_ref_val' => DBService::paramCollectionOfStrings(request('values'))
        ], PDO::PARAM_STMT);

        return response()->json($directory);
    }

    public function directoriesGroup()
    {
        $project = $this->directory(null, '1')->original;

        request()->merge([
            'refs' => ['indic'],
            'values' => collect($project)->where('disabled', '0')->pluck('id')
        ]);

        return response()->json([
            'project' => $project,
            'period' => $this->directory(null,'2')->original
        ]);
    }

    public function saveWidget()
    {
        $codes = collect(request('properties'))->flatMap(function ($values, $code) {
            return array_fill(0, is_array($values) ? count($values) : 1, $code);
        });

        $values = collect(request('properties'))->flatMap(function ($values) {
            return is_array($values) ? $values : [$values];
        });

        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.manage_widget_dashboard', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => request('widget'),
            'p_widget_type_id' => request('type'),
            'c_prop_code' => DBService::paramCollectionOfStrings($codes),
            'c_prop_value' => DBService::paramCollectionOfStrings($values)
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->getOriginalContent()
        ]);
    }

    public function remove($id): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.delete_widget', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => $id
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function copy($id): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.copy_widget_on_dashboard', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => $id
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function renameGroup(): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.change_group_name', [
            'p_user_id' => auth()->id(),
            'p_group_id' => request('group'),
            'p_group_name' => request('name')
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function removeGroup($id): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.delete_group', [
            'p_user_id' => auth()->id(),
            'p_group_id' => $id
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function createGroup(): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.create_group_on_dashboard', [
            'p_user_id' => auth()->id(),
            'p_group_name' => 'Новая группа'
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function createBasicGroup(): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.add_basic_set_widgets', [
            'p_user_id' => auth()->id(),
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function createTemplateGroup(): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.add_widgets_by_template', [
            'p_user_id' => auth()->id(),
            'c_indic_id' => DBService::paramCollectionOfStrings(request('project')),
            'c_per_id' => DBService::paramCollectionOfStrings(request('period'))
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function changeGroupOrder(): JsonResponse
    {
        $result = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.change_group_orderby', [
            'p_user_id' => auth()->id(),
            'p_group_id' => DBService::paramCollectionOfNumber(request('groups'))
        ]);

        return response()->json($result);
    }

    public function changeWidgetOrder(): JsonResponse
    {
        $result = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.change_widget_orderby', [
            'p_user_id' => auth()->id(),
            'p_group_parent_id' => request('group'),
            'p_widget_id' => DBService::paramCollectionOfNumber(request('widgets'))
        ], PDO::PARAM_STR, 4000);

        return response()->json($result);
    }

    public function saveToLibrary(): JsonResponse
    {
        $func = request('is_basic') ? 'msk_arm_lead.pkg_widget.save_widget_to_library' : 'msk_arm_lead.pkg_widget.save_widget_to_user_library';

        $message = DBService::instance()->executeFunction($func, [
            'p_user_id' => auth()->id(),
            'p_widget_id' => request('widget'),
            'p_widget_name' => request('name')
        ], PDO::PARAM_STR, 4000);

        return response()->json(['message' => $message]);
    }

    public function library(): JsonResponse
    {
        $headers = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_library_header', [
            'p_user_id' => auth()->id()
        ], PDO::PARAM_STMT);

        $rows = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.get_library', [
            'p_user_id' => auth()->id(),
            'p_widget_types' => DBService::paramCollectionOfNumber(request('types'))
        ], PDO::PARAM_STMT);

        return response()->json(['headers' => $headers, 'rows' => $rows]);
    }

    public function addFromLibrary($widgetId): JsonResponse
    {
        $message = DBService::instance()->executeFunction('msk_arm_lead.pkg_widget.add_widget_from_library', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => $widgetId
        ], PDO::PARAM_STR, 4000);

        return response()->json([
            'message' => $message,
            'widgets' => $this->widgets()->original
        ]);
    }

    public function changeWidgetLevel($widgetId): JsonResponse
    {
        DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_widget_level', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => $widgetId,
            'p_level' => request('level')
        ]);

        if (request('np')) {
            DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_widget_level_np', [
                'p_user_id' => auth()->id(),
                'p_widget_id' => $widgetId,
                'p_level_np' => request('np')
            ]);
        }

        return response()->json('success');
    }

    public function changeWidgetNpHidden ($widgetId) : JsonResponse
    {
        DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_np_hidden_props', [
            'p_widget_id' => $widgetId,
            'p_np_id' => request('np'),
            'p_rp_id' => request('rp') === 'null' ? null : request('rp'),
            'p_is_hidden' => request('hidden')
        ]);

        return response()->json('success');
    }

    public function changeVisibilityAll($widgetId): JsonResponse
    {
        DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_np_hidden_props', [
            'p_widget_id' => $widgetId,
            'p_np_id' => DBService::paramCollectionOfNumber(request('np')),
            'p_rp_id' => DBService::paramCollectionOfNumber(request('rp')),
            'p_is_hidden' => DBService::paramCollectionOfNumber(request('hidden'))
        ]);

        return response()->json();
    }

    public function changeWidgetViewType($widgetId): JsonResponse
    {
        DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_widget_view_type', [
            'p_user_id' => auth()->id(),
            'p_widget_id' => $widgetId,
            'p_view_type' => request('value')
        ]);

        return response()->json('success');
    }

    public function changeOpenGroup(): JsonResponse
    {
        DBService::instance()->executeProcedure('msk_arm_lead.pkg_widget.save_open_group', [
            'p_group_id' => request('groupId'),
            'is_opened' => request('value')
        ]);

        return response()->json('success');
    }
}
