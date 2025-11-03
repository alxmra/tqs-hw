import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '20s', target: 5 },
    { duration: '40s', target: 10 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    errors: ['rate<0.15'],
    checks: ['rate>0.85'],
  },
};

export default function () {
  const municipalities = getMunicipalities();
  
  if (municipalities && municipalities.length > 0) {
    const municipality = municipalities[Math.floor(Math.random() * municipalities.length)];
    
    const token = createBooking(municipality);
    
    if (token) {
      sleep(1);
      getBooking(token);
      sleep(1);
      
      const rand = Math.random();
      if (rand < 0.5) {
        updateBookingState(token, 'ASSIGNED');
      } else {
        deleteBooking(token);
      }
    }
  }
  
  if (Math.random() > 0.8) {
    getAllBookings();
  }
  
  sleep(1);
}

function getMunicipalities() {
  const res = http.get(`${BASE_URL}/municipalities`);
  const passed = check(res, {
    'municipalities status is 200': (r) => r.status === 200,
  });
  errorRate.add(!passed);
  
  if (res.status === 200) {
    try {
      return JSON.parse(res.body);
    } catch (e) {
      return [];
    }
  }
  return [];
}

function createBooking(municipality) {
  const today = new Date();
  const futureDate = new Date(today);
  futureDate.setDate(today.getDate() + Math.floor(Math.random() * 60) + 20);
  
  while (futureDate.getDay() === 0 || futureDate.getDay() === 6) {
    futureDate.setDate(futureDate.getDate() + 1);
  }
  
  const dateStr = futureDate.toISOString().split('T')[0];
  const timeSlots = ['09:00:00', '10:00:00', '11:00:00', '14:00:00', '15:00:00'];
  const timeSlot = timeSlots[Math.floor(Math.random() * timeSlots.length)];
  
  const payload = JSON.stringify({
    date: dateStr,
    approxTimeSlot: timeSlot,
    items: [{ itemType: 'FURNITURE', quantity: 1 }],
    municipality: municipality,
  });

  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(`${BASE_URL}/bookings`, payload, params);
  
  const passed = check(res, {
    'create booking status is 201': (r) => r.status === 201,
  });
  errorRate.add(!passed);
  
  if (res.status === 201) {
    try {
      return JSON.parse(res.body).token;
    } catch (e) {
      return null;
    }
  }
  return null;
}

function getBooking(token) {
  const res = http.get(`${BASE_URL}/bookings/${token}`);
  const passed = check(res, {
    'get booking status is 200': (r) => r.status === 200,
  });
  errorRate.add(!passed);
}

function updateBookingState(token, state) {
  const payload = JSON.stringify({ state: state });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.patch(`${BASE_URL}/bookings/${token}/state`, payload, params);
  const passed = check(res, {
    'update state status is 204': (r) => r.status === 204,
  });
  errorRate.add(!passed);
}

function deleteBooking(token) {
  const res = http.del(`${BASE_URL}/bookings/${token}`);
  const passed = check(res, {
    'delete booking status is 204': (r) => r.status === 204,
  });
  errorRate.add(!passed);
}

function getAllBookings() {
  const res = http.get(`${BASE_URL}/staff/bookings`);
  const passed = check(res, {
    'get all bookings status is 200': (r) => r.status === 200,
  });
  errorRate.add(!passed);
}