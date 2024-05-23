<?php

namespace App\Http\Controllers;

use App\Services\RemoteEventsInterface;
use Carbon\Carbon;
use Illuminate\Http\Request;

class BetFairController extends Controller
{
    protected $remoteEvents;

    public function __construct(RemoteEventsInterface $remoteEvents)
    {
        $this->remoteEvents = $remoteEvents;
    }

    public function listEvents(Request $request)
    {
        $dateFrom = new Carbon();
        $dateTo   = (clone $dateFrom)->addDays(1);

        return response()->json($this->remoteEvents->listEventsByDateRange($dateFrom, $dateTo));
    }
}
