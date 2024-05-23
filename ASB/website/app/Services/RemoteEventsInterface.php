<?php


namespace App\Services;


use Carbon\Carbon;

interface RemoteEventsInterface
{
    public function testina();

    public function listEventsByDateRange(Carbon $dateFrom, Carbon $dateTo);
}
