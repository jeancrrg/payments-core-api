INSERT INTO customers (id, name, cpf, created_at, updated_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Alice Souza',  '52998224725', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Bob Ferreira', '07750637516', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Carol Lima',   '71428793860', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Dave Rocha',   '87748242096', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

INSERT INTO payments (id, customer_id, stripe_payment_intent_id, amount, currency, status, description, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111101', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'pi_test_alice_001', 199.90, 'USD', 'SUCCEEDED',         'Monthly subscription - January',              '2026-01-15T10:00:00Z', '2026-01-15T10:00:05Z'),
    ('11111111-1111-1111-1111-111111111102', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'pi_test_alice_002', 199.90, 'USD', 'SUCCEEDED',         'Monthly subscription - February',             '2026-02-15T10:00:00Z', '2026-02-15T10:00:05Z'),
    ('11111111-1111-1111-1111-111111111103', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'pi_test_alice_003', 199.90, 'USD', 'PARTIALLY_REFUNDED','Monthly subscription - March (partial refund)','2026-03-15T10:00:00Z', '2026-03-20T14:00:00Z'),
    ('11111111-1111-1111-1111-111111111104', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'pi_test_alice_004', 199.90, 'USD', 'REFUNDED',          'Monthly subscription - April (refunded)',      '2026-04-15T10:00:00Z', '2026-04-18T09:00:00Z'),
    ('22222222-2222-2222-2222-222222222201', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'pi_test_bob_001',   49.00,  'USD', 'SUCCEEDED',         'One-time purchase',                           '2026-02-10T08:30:00Z', '2026-02-10T08:30:03Z'),
    ('22222222-2222-2222-2222-222222222202', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'pi_test_bob_002',   75.50,  'USD', 'FAILED',            'Insufficient funds',                          '2026-03-05T12:00:00Z', '2026-03-05T12:00:02Z'),
    ('22222222-2222-2222-2222-222222222203', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL,                12.00,  'USD', 'PENDING',           'Awaiting confirmation',                       '2026-05-01T09:00:00Z', '2026-05-01T09:00:00Z'),
    ('33333333-3333-3333-3333-333333333301', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'pi_test_carol_001', 1500.00,'EUR', 'SUCCEEDED',         'Annual plan',                                 '2026-01-20T16:00:00Z', '2026-01-20T16:00:04Z'),
    ('33333333-3333-3333-3333-333333333302', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'pi_test_carol_002', 250.00, 'EUR', 'CANCELLED',         'Customer cancelled before capture',           '2026-02-25T11:00:00Z', '2026-02-25T11:05:00Z'),
    ('44444444-4444-4444-4444-444444444401', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'pi_test_dave_001',  9.99,   'BRL', 'PROCESSING',        'Awaiting Stripe processing',                  '2026-05-03T18:45:00Z', '2026-05-03T18:45:01Z');

INSERT INTO refund_transactions (id, payment_id, stripe_refund_id, amount, reason, status, created_at, updated_at) VALUES
    ('55555555-5555-5555-5555-555555555501', '11111111-1111-1111-1111-111111111103', 're_test_alice_003_partial', 50.00,  'requested_by_customer', 'SUCCEEDED', '2026-03-20T14:00:00Z', '2026-03-20T14:00:03Z'),
    ('55555555-5555-5555-5555-555555555502', '11111111-1111-1111-1111-111111111104', 're_test_alice_004_full',    199.90, 'requested_by_customer', 'SUCCEEDED', '2026-04-18T09:00:00Z', '2026-04-18T09:00:03Z');
