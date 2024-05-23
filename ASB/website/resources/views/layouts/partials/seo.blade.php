<?php
// defaults values

if (isset($title)) {
    $title .= ' - ' . config('app.name');
} else {
    $title = config('app.name');
}

$image = $image ?? asset('images/logo/logo-asb-type.png');
?>

{{-- Classic SEO tag and new open graph tags --}}

<title>{{$title}}</title>
<meta name="author" content="{{ config('company.name') }}">

@isset($description)
    <meta name="description" content="{{ $description }}">
    <meta property="twitter:description" content="{{ $description }}">
@endisset

@isset($keywords)
    <meta name="keywords" content="{{ implode(', ', $keywords) }}">
@endisset

<meta property="og:type" content="website">
<meta property="og:title" content="{{$title}}">
<meta property="og:url" content="{{ Request::url() }}">
<meta property="og:site_name" content="{{config('app.name')}}">
<meta property="og:image" content="{{$image}}">

<meta property="twitter:title" content="{{$title}}">
<meta property="twitter:creator" content="{{config('company.social.twitter')}}">
<meta property="twitter:card" content="summary">
<meta property="twitter:site" content="{{config('app.url')}}">
<meta property="twitter:image" content="{{$image}}">
