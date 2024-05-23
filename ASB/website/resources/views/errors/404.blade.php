@extends('layouts.page-error')

@section('content')
    <div class="row justify-content-center align-items-center h-100">
        <div class="col-12">
            <!-- Begin Container -->
            <div class="error-container mx-auto text-center">
                <h1>404</h1>
                <h2>This page cannot be found! </h2>
                <p>But we have lots of other pages for you to see. </p>
                <a href="javascript:history.back()" class="btn btn-shadow">
                    Go Back
                </a>
            </div>
            <!-- End Container -->
        </div>
        <!-- End Col -->
    </div>
@endsection
