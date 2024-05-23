@extends('layouts.page-simple')

@section('content')
    <div class="row">
        <!-- Begin Events -->
        <div class="col-xl-3">
            <div class="widget has-shadow">
                <div class="widget-header bordered no-actions">
                    <h2>Draggable Events</h2>
                </div>
                <div class="widget-body">
                    <div id="external-events">
                        <div class="remove-drop">
                            <div class="styled-checkbox">
                                <input type="checkbox" name="drop-remove" id="drop-remove">
                                <label for="drop-remove">Remove after drop</label>
                            </div>
                        </div>
                        <div class="em-separator separator-dashed"></div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default mt-0">
                                <div class="fc-content"><span class="fc-title"><i class="la la-scissors"></i>Barber</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-birthday-cake"></i>Birthday</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-cutlery"></i>Food</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-glass"></i>Restaurant</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-graduation-cap"></i>School</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-medkit"></i>Medical</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-suitcase"></i>Work</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-plane"></i>Travel</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-futbol-o"></i>Sports</span></div>
                            </div>
                        </div>
                        <div class="fc-event-container">
                            <div class="fc-event fc-bg-default">
                                <div class="fc-content"><span class="fc-title"><i class="la la-child"></i>Baby Shower</span></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- End Events -->
        <div class="col-xl-9">
            <!-- Begin Widget -->
            <div class="widget has-shadow">
                <div class="widget-header bordered d-flex align-items-center">
                    <h2>Calendar</h2>
                    <div class="widget-options">
                        <div class="dropdown">
                            <button type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="dropdown-toggle">
                                <i class="la la-ellipsis-h"></i>
                            </button>
                            <div class="dropdown-menu">
                                <a href="#" class="dropdown-item">
                                    Add Event
                                </a>
                                <a href="app-calendar.html" class="dropdown-item">
                                    Basic Calendar
                                </a>
                                <a href="app-calendar-list.html" class="dropdown-item">
                                    List Views
                                </a>
                                <a href="app-calendar-event.html" class="dropdown-item">
                                    External Events
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- End Widget Header -->
                <!-- Begin Widget Body -->
                <div class="widget-body">
                    <!-- Begin Calendar -->
                    <div id="calendar-container">
                        <div id="calendar"></div>
                    </div>
                    <!-- End Calendar -->
                </div>
                <!-- End Calendar -->
            </div>
            <!-- End Widget -->
        </div>
        <!-- End Col -->
    </div>
@endsection

@push('scripts')
    <script src="{{ asset('js/event-calendar.js')}}"></script>
@endpush
