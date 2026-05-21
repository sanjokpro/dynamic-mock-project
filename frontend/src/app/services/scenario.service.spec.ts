import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ScenarioService } from './scenario.service';
import { ScenarioResponse, ScenarioRequest } from '../models/route.model';

describe('ScenarioService', () => {
  let service: ScenarioService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/scenarios';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ScenarioService]
    });
    service = TestBed.inject(ScenarioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getScenarios', () => {
    it('should fetch all scenarios', () => {
      const mockScenarios: ScenarioResponse[] = [
        { id: '1', name: 'checkout-flow', initialState: 'cart', states: [], active: true },
        { id: '2', name: 'auth-flow', initialState: 'logged-out', states: [], active: false }
      ];

      service.getScenarios().subscribe(scenarios => {
        expect(scenarios).toEqual(mockScenarios);
        expect(scenarios.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockScenarios);
    });
  });

  describe('getScenario', () => {
    it('should fetch a single scenario by id', () => {
      const mockScenario: ScenarioResponse = {
        id: '1',
        name: 'checkout-flow',
        initialState: 'cart',
        currentState: 'payment',
        states: [
          { name: 'cart', responseTemplate: '{"status": "cart"}' },
          { name: 'payment', responseTemplate: '{"status": "payment"}' }
        ],
        active: true
      };

      service.getScenario('1').subscribe(scenario => {
        expect(scenario).toEqual(mockScenario);
        expect(scenario.currentState).toBe('payment');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockScenario);
    });
  });

  describe('createScenario', () => {
    it('should create a new scenario', () => {
      const createRequest: ScenarioRequest = {
        name: 'new-flow',
        initialState: 'start',
        states: [
          { name: 'start', responseTemplate: '{"step": 1}' },
          { name: 'end', responseTemplate: '{"step": 2}' }
        ]
      };

      const mockResponse: ScenarioResponse = {
        id: 'new-id',
        ...createRequest,
        currentState: 'start',
        active: false
      };

      service.createScenario(createRequest).subscribe(scenario => {
        expect(scenario.id).toBe('new-id');
        expect(scenario.name).toBe('new-flow');
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('resetScenario', () => {
    it('should reset a scenario to initial state', () => {
      const mockResponse: ScenarioResponse = {
        id: '1',
        name: 'checkout-flow',
        initialState: 'cart',
        currentState: 'cart',
        states: [],
        active: true,
        executionCount: 0
      };

      service.resetScenario('1').subscribe(scenario => {
        expect(scenario.currentState).toBe('cart');
        expect(scenario.executionCount).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/reset`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getCurrentState', () => {
    it('should get current state of a scenario', () => {
      const mockResponse = {
        scenarioName: 'checkout-flow',
        currentState: 'payment'
      };

      service.getCurrentState('1').subscribe(result => {
        expect(result.currentState).toBe('payment');
      });

      const req = httpMock.expectOne(`${apiUrl}/1/state`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('triggerTransition', () => {
    it('should trigger a state transition', () => {
      const mockResponse = {
        scenarioName: 'checkout-flow',
        previousState: 'cart',
        currentState: 'payment'
      };

      service.triggerTransition('1', { trigger: 'proceed' }).subscribe(result => {
        expect(result.previousState).toBe('cart');
        expect(result.currentState).toBe('payment');
      });

      const req = httpMock.expectOne(`${apiUrl}/1/transition`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ trigger: 'proceed' });
      req.flush(mockResponse);
    });
  });
});

