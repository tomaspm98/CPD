#pragma once

#define NUMBER_OF_PAPI_EVENTS 4

#define start_papi_event_counter(EventSet) \
  if (PAPI_start(EventSet) != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

#define stop_papi_event_counter(EventSet, values) \
  if (PAPI_stop(EventSet, values) != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;

#define reset_papi_event_counter(EventSet) \
  if (PAPI_reset(EventSet) != PAPI_OK) std::cout << "FAIL reset" << endl;