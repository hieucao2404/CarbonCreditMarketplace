-- Find vehicles for evowner2
SELECT 
    v.vehicle_id, 
    v.vin, 
    v.model, 
    u.username,
    u.user_id as owner_id
FROM vehicles v
JOIN users u ON v.user_id = u.user_id
WHERE u.username = 'evowner2';
