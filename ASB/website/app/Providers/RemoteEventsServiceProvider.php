<?php

namespace App\Providers;

use App\Services\BetFairConnector;
use App\Services\RemoteEventsInterface;
use Illuminate\Contracts\Support\DeferrableProvider;
use Illuminate\Support\ServiceProvider;

class RemoteEventsServiceProvider extends ServiceProvider implements DeferrableProvider
{
    /**
     * Register services.
     *
     * @return void
     */
    public function register()
    {
        $this->app->singleton(RemoteEventsInterface::class, BetFairConnector::class);
    }

    /**
     * Bootstrap services.
     *
     * @return void
     */
    public function boot()
    {
        //
    }

    public function provides()
    {
        return [BetFairConnector::class];
    }
}
