import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const failureRate = new Rate('failed_requests');

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // Warm up to 50 users
    { duration: '10s', target: 50 },   // Stay at 50 users
    { duration: '10s', target: 500 },  // SPIKE to 500 users
    { duration: '40s', target: 500 },  // Stay at 500 users
    { duration: '10s', target: 50 },   // Drop back to 50 users
    { duration: '10s', target: 50 },   // Recover at 50 users
    { duration: '20s', target: 0 },    // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    failed_requests: ['rate<0.3'],
  },
};

const BASE_URL = 'http://localhost:8080/api';
const municipalities = ['Lisboa', 'Porto', 'Coimbra', 'Braga', 'Faro', 'Aveiro', 'Funchal', 'Setúbal', 'Évora', 'Viseu'];

const itemTemplates = [
  { name: 'Mattress', description: 'Old king-size mattress' },
  { name: 'Refrigerator', description: 'Broken fridge' },
  { name: 'Sofa', description: 'Used 3-seater sofa' },
  { name: 'TV', description: 'Old television set' },
  { name: 'Washing Machine', description: 'Non-functional washer' },
  { name: 'Table', description: 'Wooden dining table' },
  { name: 'Chair', description: 'Office chair with wheels' },
  { name: 'Microwave', description: 'Old microwave oven' },
  { name: 'Bed Frame', description: 'Metal bed frame' },
  { name: 'Desk', description: 'Wooden office desk' },
];

function getRandomItems() {
  const numItems = Math.floor(Math.random() * 5) + 1; // 1-5 items
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
  const hours = [9, 10, 11, 12, 14, 15, 16, 17];
  const hour = hours[Math.floor(Math.random() * hours.length)];
  return `${hour.toString().padStart(2, '0')}:00:00`;
}

export default function () {
  // Test 1: Quick municipalities check
  let res = http.get(`${BASE_URL}/municipalities`);
  check(res, {
    'get municipalities status 200': (r) => r.status === 200,
  }) || failureRate.add(1);
  
  sleep(0.2);

  // Test 2: Rapid booking creation
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

  sleep(0.1);

  // Test 3: Heavy reads by municipality
  res = http.get(`${BASE_URL}/municipalities/${municipality}`);
  check(res, {
    'get bookings by municipality status 200': (r) => r.status === 200,
  }) || failureRate.add(1);

  sleep(0.1);

  // Test 4: Concurrent state queries
  const states = ['RECEIVED', 'ASSIGNED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED', 'REMOVED'];
  const randomState = states[Math.floor(Math.random() * states.length)];
  res = http.get(`${BASE_URL}/bookings/state/${randomState}`);
  check(res, {
    'get bookings by state status 200': (r) => r.status === 200,
  }) || failureRate.add(1);

  sleep(0.1);

  // Test 5: Token-based operations during spike
  if (token) {
    res = http.get(`${BASE_URL}/bookings/${token}`);
    check(res, {
      'check booking status 200': (r) => r.status === 200,
    }) || failureRate.add(1);

    sleep(0.1);

    // Modify state rapidly
    const statePayload = JSON.stringify({
      state: states[Math.floor(Math.random() * states.length)],
    });
    res = http.patch(`${BASE_URL}/bookings/${token}/state`, statePayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    check(res, {
      'modify booking state status 200 or 204': (r) => r.status === 200 || r.status === 204,
    }) || failureRate.add(1);
  }

  // Test 6: Additional read during peak
  const randomMunicipality = municipalities[Math.floor(Math.random() * municipalities.length)];
  res = http.get(`${BASE_URL}/municipalities/${randomMunicipality}`);
  check(res, {
    'additional municipality read status 200': (r) => r.status === 200,
  }) || failureRate.add(1);

  sleep(0.2);
}