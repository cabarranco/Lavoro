<?php


namespace App\Services;


use App\Exceptions\BetFairLoginException;
use Carbon\Carbon;

class BetFairConnector implements RemoteEventsInterface
{
    // connector general parameters
    protected const BASE_URL       = 'https://api.betfair.com/exchange/betting/rest/v1.0';
    protected const BASE_URL_LOGIN = 'https://identitysso.betfair.com/api';

    // licence data
    static protected $applicationKey, $username, $password;

    // session data
    static protected $sessionToken = null;

    public function __construct()
    {
        static::$applicationKey = config('services.betfair.application_key');
        static::$username       = config('services.betfair.username');
        static::$password       = config('services.betfair.password');

        if (!self::$sessionToken) {
            $this->login();
        }
    }

    /**
     * authentication method
     *
     * @return mixed
     * @throws BetFairLoginException
     */
    protected function login()
    {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, static::BASE_URL_LOGIN . '/login');
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Expect:',
            'X-Application: ' . static::$applicationKey,
            //'X-Authentication: ' . $sessionToken,
            'Accept: application/json',
            'Content-Type: application/x-www-form-urlencoded',
        ]);

        $postData = 'username=' . static::$username . '&password=' . static::$password;

        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

        $response = json_decode(curl_exec($ch));
        curl_close($ch);

        if ($response->status == 'SUCCESS') {
            static::$sessionToken = $response->token;
        } else {
            throw new BetFairLoginException(__('BetFairConnector.errors.' . $response->error ?? 'GENERIC'));
        }

        // todo: return nothing after function fix
        return $response;
    }

    /**
     * base method for API queries
     *
     * @param string $method
     * @param array  $params
     *
     * @return mixed
     */
    protected function runMethod(string $method, $params = []): ?array
    {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, self::BASE_URL . '/' . $method . '/');
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Expect:',
            'X-Application: ' . static::$applicationKey,
            'X-Authentication: ' . static::$sessionToken,
            'Accept: application/json',
            'Content-Type: application/json',
        ]);

        $postData = json_encode($params);

        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

        $response = json_decode(curl_exec($ch));
        curl_close($ch);

        return $response;
    }

    /*
    |--------------------------------------------------------------------------
    | Methods available
    |--------------------------------------------------------------------------
    |
    | available methods useful for this application
    |
    */

    /**
     * retrieves list of events in a specified date range
     *
     * @param Carbon $dateFrom
     * @param Carbon $dateTo
     *
     * @return array
     */
    public function listEventsByDateRange(Carbon $dateFrom, Carbon $dateTo)
    {
        $filter = [
            'filter' => [
                'marketTypeCodes' => [
                    'MATCH_ODDS',
                    'OVER_UNDER_25',
                    'CORRECT_SCORE',
                ],
                'marketStartTime' => [
                    'from' => $dateFrom->toIso8601String(),
                    'to'   => $dateTo->toIso8601String(),

                ],
                'eventTypeIds'    => ['1'],
            ],
        ];

        return $this->runMethod('listEvents', $filter);
    }

    public function testina()
    {
        return "OK";
    }
}
