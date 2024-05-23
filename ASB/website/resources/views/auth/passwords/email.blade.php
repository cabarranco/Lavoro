@extends('layouts.page-wholescreen')

@section('content')
    <div class="row flex-row h-100 bg-fixed-02">
        <div class="col-12 my-auto">
            <div class="password-form mx-auto bg-dark">
                <div class="logo-centered">
                    <a href="{{route('home')}}">
                        <img src="{{asset('images/logo/logo-asb-full.png')}}" alt="logo">
                    </a>
                </div>
                <h3 class="text-grey-light">Password Recovery</h3>
                <form method="POST" action="{{ route('password.email') }}">
                    @csrf
                    <div class="group material-input">
                        <input type="email" required>
                        <span class="highlight"></span>
                        <span class="bar"></span>
                        <label>Email</label>
                    </div>
                    <div class="button text-center">
                        <button type="submit" class="btn btn-lg btn-gradient-01">
                            Send Password Reset Link
                        </button>
                    </div>
                </form>
                <div class="back">
                    <a href="{{route('login')}}">Sign In</a>
                </div>
            </div>
        </div>
        <!-- End Col -->
    </div>
@endsection
