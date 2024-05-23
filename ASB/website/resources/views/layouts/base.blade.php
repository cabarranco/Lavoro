<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">

<head>
    <meta name="csrf-token" content="{{ csrf_token() }}">

    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">

    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <!-- Google Fonts -->
    <script src="https://ajax.googleapis.com/ajax/libs/webfont/1.6.26/webfont.js"></script>
    <script>
        WebFont.load({
            google: {"families": ["Montserrat:400,500,600,700", "Noto+Sans:400,700"]},
            active: function () {
                sessionStorage.fonts = true;
            }
        });
    </script>
@include('layouts.partials.favicon')
@include('layouts.partials.styles')

<!-- Tweaks for older IEs--><!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script><![endif]-->
</head>
<body id="page-top">

@include('layouts.partials.preloader')

@section('page')
<div class="page">
    @section('header')
        @include('layouts.partials.header')
    @show

    <div class="page-content d-flex align-items-stretch">
        @section('menu')
            @include('layouts.partials.menu-sidebar')
        @show

        <div class="content-inner">
            @yield('content-wrapper')

            @section('footer')
                @include('layouts.partials.footer')
                @include('layouts.partials.scroll-to-top')
            @show
            @include('layouts.partials.offcanvas-sidebar')
        </div>
    </div>
</div>
@show

@include('layouts.partials.modal-success')
@include('layouts.partials.modal-begin')

@include('layouts.partials.scripts')
</body>
</html>
