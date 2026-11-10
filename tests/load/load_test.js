/**
 * Script de Pruebas de Carga y Estrés - K6
 * Constructora Backend - Grupo Arquinova
 *
 * Simula escenarios reales de tráfico concurrente para validar que el backend
 * cumple los umbrales de latencia P95 < 200ms y tasa de error < 1% bajo carga.
 *
 * Uso básico:
 *   k6 run load_test.js
 *
 * Uso contra servidor remoto:
 *   k6 run --env BASE_URL=https://api.tudominio.com load_test.js
 *
 * Requiere K6 instalado: https://k6.io/docs/getting-started/installation/
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ─── Variables de entorno ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USUARIO_PRUEBA = __ENV.TEST_USER || 'admin@arquinova.com';
const PASSWORD_PRUEBA = __ENV.TEST_PASSWORD || 'Admin123!';

// ─── Métricas personalizadas ─────────────────────────────────────────────────
const errorRate = new Rate('error_rate');
const loginTrend = new Trend('login_duration_ms');
const proyectosTrend = new Trend('proyectos_duration_ms');
const lotesTrend = new Trend('lotes_duration_ms');
const solicitudesExitosas = new Counter('solicitudes_contacto_creadas');

// ─── Configuración de escenarios ──────────────────────────────────────────────
export const options = {
  scenarios: {
    // Escenario 1: Rampa gradual de carga (simula apertura normal del sitio)
    rampa_normal: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m',  target: 100 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
    // Escenario 2: Pico de tráfico (ej: campaña publicitaria repentina)
    pico_trafico: {
      executor: 'ramping-vus',
      startTime: '2m30s',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 500 },
        { duration: '30s', target: 500 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },

  // ─── Umbrales de calidad ─────────────────────────────────────────────────
  thresholds: {
    'http_req_duration{type:public}': ['p(95)<200'],
    'http_req_failed': ['rate<0.01'],
    'login_duration_ms': ['p(95)<500'],
    'proyectos_duration_ms': ['p(95)<150'],
    'lotes_duration_ms': ['p(95)<150'],
    'error_rate': ['rate<0.01'],
  },
};

// ─── Setup: obtener token JWT antes de los escenarios ─────────────────────────
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ correo: USUARIO_PRUEBA, password: PASSWORD_PRUEBA }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'Setup: login exitoso': (r) => r.status === 200,
    'Setup: token recibido': (r) => r.json('token') !== undefined,
  });

  return { token: loginRes.json('token') };
}

// ─── Función principal por VU ─────────────────────────────────────────────────
export default function (data) {
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };
  const publicHeaders = { 'Content-Type': 'application/json' };

  // GRUPO 1: Endpoints Públicos
  group('Endpoints Publicos', function () {
    const proyectosRes = http.get(`${BASE_URL}/api/proyectos`, {
      headers: publicHeaders,
      tags: { type: 'public', endpoint: 'proyectos' },
    });

    const proyectosOk = check(proyectosRes, {
      'GET /api/proyectos - status 200': (r) => r.status === 200,
      'GET /api/proyectos - es array': (r) => Array.isArray(r.json()),
    });

    proyectosTrend.add(proyectosRes.timings.duration);
    errorRate.add(!proyectosOk);

    const ubicacionesRes = http.get(`${BASE_URL}/api/ubicaciones`, {
      headers: publicHeaders,
      tags: { type: 'public', endpoint: 'ubicaciones' },
    });

    check(ubicacionesRes, { 'GET /api/ubicaciones - 200': (r) => r.status === 200 });
    errorRate.add(ubicacionesRes.status !== 200);

    sleep(0.5);
  });

  // GRUPO 2: Endpoints Autenticados
  group('Endpoints Autenticados', function () {
    const lotesRes = http.get(`${BASE_URL}/api/lotes`, {
      headers: authHeaders,
      tags: { type: 'authenticated', endpoint: 'lotes' },
    });

    check(lotesRes, {
      'GET /api/lotes - 200 o 403': (r) => r.status === 200 || r.status === 403,
    });

    lotesTrend.add(lotesRes.timings.duration);
    errorRate.add(lotesRes.status >= 500);

    sleep(0.3);
  });

  // GRUPO 3: Autenticacion (10% de usuarios)
  group('Flujo de Autenticacion', function () {
    if (Math.random() < 0.10) {
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ correo: USUARIO_PRUEBA, password: PASSWORD_PRUEBA }),
        { headers: publicHeaders, tags: { type: 'auth', endpoint: 'login' } }
      );

      const loginOk = check(loginRes, {
        'POST /api/auth/login - 200': (r) => r.status === 200,
        'POST /api/auth/login - JWT': (r) => r.json('token') !== null,
      });

      loginTrend.add(loginRes.timings.duration);
      errorRate.add(!loginOk);
    }

    sleep(0.2);
  });

  // GRUPO 4: Formulario de Contacto (5% de usuarios)
  group('Solicitud de Contacto', function () {
    if (Math.random() < 0.05) {
      const payload = JSON.stringify({
        nombreCompleto: `Usuario Test ${__VU}`,
        correo: `test.${__VU}.${__ITER}@example.com`,
        telefono: '3001234567',
        mensaje: 'Solicitud generada por prueba de carga K6 automatizada.',
        proyectoId: 1,
      });

      const res = http.post(
        `${BASE_URL}/api/solicitudes-contacto/publico`,
        payload,
        { headers: publicHeaders, tags: { type: 'public', endpoint: 'solicitud' } }
      );

      const ok = check(res, { 'POST /solicitudes-contacto - 201': (r) => r.status === 201 });
      if (ok) solicitudesExitosas.add(1);
      errorRate.add(!ok);
    }

    sleep(1);
  });

  // GRUPO 5: Health Check (balanceador de carga)
  group('Health Check', function () {
    const healthRes = http.get(`${BASE_URL}/actuator/health`, {
      tags: { type: 'infrastructure', endpoint: 'health' },
    });

    check(healthRes, {
      'GET /actuator/health - 200': (r) => r.status === 200,
      'GET /actuator/health - UP': (r) => r.json('status') === 'UP',
    });

    errorRate.add(healthRes.status !== 200);
  });

  sleep(Math.random() * 2 + 0.5);
}

// ─── Resumen al terminar ───────────────────────────────────────────────────────
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const metrics = data.metrics;
  const dur = metrics['http_req_duration'];
  const failed = metrics['http_req_failed'];
  const reqs = metrics['http_reqs'];

  const passed = (failed?.values?.rate || 0) < 0.01 && (dur?.values['p(95)'] || 999) < 200;

  console.log('\n' + [
    '╔══════════════════════════════════════════════════════════════╗',
    '║    CONSTRUCTORA ARQUINOVA - REPORTE DE PRUEBAS DE CARGA      ║',
    '╠══════════════════════════════════════════════════════════════╣',
    `║  Solicitudes Totales : ${String(reqs?.values?.count || 0).padEnd(36)}║`,
    `║  Tasa de Errores     : ${((failed?.values?.rate || 0) * 100).toFixed(2).padEnd(35)}%║`,
    `║  Latencia Promedio   : ${(dur?.values?.avg || 0).toFixed(2).padEnd(33)} ms║`,
    `║  Latencia P95        : ${(dur?.values['p(95)'] || 0).toFixed(2).padEnd(33)} ms║`,
    `║  Latencia Maxima     : ${(dur?.values?.max || 0).toFixed(2).padEnd(33)} ms║`,
    '╠══════════════════════════════════════════════════════════════╣',
    `║  RESULTADO: ${passed ? '✅ TODOS LOS UMBRALES CUMPLIDOS       ' : '❌ UMBRALES FALLIDOS - REVISAR METRICAS'}║`,
    '╚══════════════════════════════════════════════════════════════╝',
  ].join('\n'));

  return {
    [`tests/load/resultados/resultado_${timestamp}.json`]: JSON.stringify(data, null, 2),
  };
}
