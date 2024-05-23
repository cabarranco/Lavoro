@extends('layouts.base')

@section('content-wrapper')
    <div class="container-fluid">
        @include('layouts.partials.page-header')

            @yield('content')
    </div>
@endsection
