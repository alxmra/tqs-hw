import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const failureRate = new Rate('failed_requests');

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp up to 50 users
    { duration: '60s', target: 50 },   // Stay at 50 users
    { duration: '20s', target: 0 },    // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    failed_requests: ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080/api';
const municipalities = ['Lisboa', 'Porto', 'Coimbra', 'Braga', 'Faro'];

const itemTemplates = [
  { name: 'Mattress', description: 'Old king-size mattress' },
  { name: 'Refrigerator', description: 'Broken fridge' },
  { name: 'Sofa', description: 'Used 3-seater sofa' },
  { name: 'TV', description: 'Old television set' },
  { name: 'Washing Machine', description: 'Non-functional washer' },
  { name: 'Table', description: 'Wooden dining table' },
  { name: 'Chair', description: 'Office chair with wheels' },
];

function getRandomItems() {
  const numItems = Math.floor(Math.random() * 3) + 1; // 1-3 items
  const items = [];
  for (let i = 0; i < numItems; i++) {
    const template = itemTemplates[Math.floor(Math.random() * itemTemplates.length)];
    items.push({
      name: template.name,
      description: template.description
    });
  }
  return items;
}

function getRandomTimeSlot() {
  const hours = [9, 10, 11, 14, 15, 16];
  const hour = hours[Math.floor(Math.random() * hours.length)];
  return `${hour.toString().padStart(2, '0')}:00:00`;
}

export default function () {
  // Test 1: Get municipalities list
  let res = http.get(`${BASE_URL}/municipalities`);
  check(res, {
    'get municipalities status 200': (r) => r.status === 200,
  }) || failureRate.add(1);
  
  sleep(1);

  // Test 2: Create a booking
  const municipality = municipalities[Math.floor(Math.random() * municipalities.length)];
  const futureDate = new Date(Date.now() + 86400000 * (Math.floor(Math.random() * 30) + 1));
  const dateStr = futureDate.toISOString().split('T')[0];
  
  const bookingPayload = JSON.stringify({
    date: dateStr,
    approxTimeSlot: getRandomTimeSlot(),
    items: getRandomItems(),
    municipality: municipality
  });

  res = http.post(`${BASE_URL}/bookings`, bookingPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  
  const bookingSuccess = check(res, {
    'create booking status 201': (r) => r.status === 201,
  });
  
  if (!bookingSuccess) {
    failureRate.add(1);
  }

  let token = null;
  if (res.status === 201 && res.body) {
    try {
      const body = JSON.parse(res.body);
      token = body.token;
    } catch (e) {
      // Ignore parse errors
    }
  }

  sleep(1);

  // Test 3: Get bookings by municipality
  res = http.get(`${BASE_URL}/municipalities/${municipality}`);
  check(res, {
    'get bookings by municipality status 200': (r) => r.status === 200,
  }) || failureRate.add(1);

  sleep(1);

  // Test 4: Get bookings by state
  const states = ['RECEIVED', 'ASSIGNED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED', 'REMOVED'];
  const randomState = states[Math.floor(Math.random() * states.length)];
  res = http.get(`${BASE_URL}/bookings/state/${randomState}`);
  check(res, {
    'get bookings by state status 200': (r) => r.status === 200,
  }) || failureRate.add(1);

  sleep(1);

  // Test 5: Check booking by token (if we have one)
  if (token) {
    res = http.get(`${BASE_URL}/bookings/${token}`);
    check(res, {
      'check booking status 200': (r) => r.status === 200,
    }) || failureRate.add(1);
  }

  sleep(1);
}