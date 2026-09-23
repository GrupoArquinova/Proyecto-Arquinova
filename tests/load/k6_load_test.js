import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuración de la prueba de carga
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Rampa de subida a 20 usuarios
    { duration: '1m', target: 50 },   // Carga constante de 50 usuarios concurrentes
    { duration: '30s', target: 100 },  // Pico de estrés a 100 usuarios concurrentes
    { duration: '30s', target: 0 },   // Rampa de bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'], // El 95% de las peticiones debe responder en < 300ms
    http_req_failed: ['rate<0.01'],    // Menos del 1% de errores tolerados
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // 1. Health check de infraestructura
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'Health check responde 200': (r) => r.status === 200,
  });

  // 2. Catálogo público de proyectos (con soporte de caché)
  const proyectosRes = http.get(`${BASE_URL}/api/proyectos`);
  check(proyectosRes, {
    'Listar proyectos responde 200': (r) => r.status === 200,
    'Header X-Correlation-ID presente': (r) => r.headers['X-Correlation-ID'] !== undefined,
  });

  // 3. Catálogo público de ubicaciones (con soporte de caché)
  const ubicacionesRes = http.get(`${BASE_URL}/api/ubicaciones`);
  check(ubicacionesRes, {
    'Listar ubicaciones responde 200': (r) => r.status === 200,
  });

  sleep(1);
}
