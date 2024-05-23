@extends('layouts.page-wholescreen')

@section('content')
    <div class="row flex-row h-100">
        <!-- Begin Left Content -->
        <div class="col-xl-8 col-lg-6 col-md-5 no-padding">
            <div class="elisyam-bg background-01">
                <div class="elisyam-overlay overlay-01"></div>
                <div class="authentication-col-content mx-auto">
                    <h1 class="gradient-text-01">
                        Welcome To {{config('app.name')}}!
                    </h1>
                    <span class="description">
                                Etiam consequat urna at magna bibendum, in tempor arcu fermentum vitae mi massa egestas.
                            </span>
                </div>
            </div>
        </div>
        <!-- End Left Content -->
        <!-- Begin Right Content -->
        <div class="col-xl-4 col-lg-6 col-md-7 my-auto no-padding">
            <!-- Begin Form -->
            <div class="authentication-form mx-auto">
                <div class="logo-centered">
                    <a href="{{route('home')}}">
                        <img src="{{asset('images/logo/logo-asb-full.png')}}" alt="logo">
                    </a>
                </div>
                <h3 class="text-grey-light">Sign In To {{config('app.name')}}</h3>
                <form action="{{route('login')}}" method="post">
                    @csrf
                    <div class="group material-input">
                        <input name="email" required type="email">
                        <span class="highlight"></span>
                        <span class="bar"></span>
                        <label>Email</label>
                    </div>
                    <div class="group material-input">
                        <input name="password" type="password" required>
                        <span class="highlight"></span>
                        <span class="bar"></span>
                        <label>Password</label>
                    </div>
                    <div class="row">
                        <div class="col text-left">
                            <div class="styled-checkbox">
                                <input type="checkbox" name="remember" id="remember">
                                <label for="remember">Remember me</label>
                            </div>
                        </div>
                        <div class="col text-right">
                            <a href="{{route('password.request')}}">Forgot Password ?</a>
                        </div>
                    </div>
                    <div class="sign-btn text-center">
                        <button type="submit" class="btn btn-lg btn-gradient-01">
                            Sign in
                        </button>
                    </div>
                </form>
                <div class="register">
                    Don't have an account?
                    <br>
                    <a href="{{route('register')}}">Create an account</a>
                </div>
            </div>
            <!-- End Form -->
        </div>
        <!-- End Right Content -->
    </div>
@endsection
