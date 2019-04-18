const hazelcast = require('./hazelcast');
const haversine = require('haversine');

exports.handle = async (request, context, callback) => {
    console.log('Got request: ' + JSON.stringify(request));
    context.callbackWaitsForEmptyEventLoop = false;

    let userId = request.userId;

    let hazelcastClient = await hazelcast.getClient();
    let airports = await hazelcastClient.getMap('airports');
    if (await airports.isEmpty()) {
        return callback('Airports data is not initialized', null);
    }
    let users = await hazelcastClient.getMap('users');

    let user = await users.get(userId);
    if (!user) {
        await users.set(userId, {
            userId: userId,
            lastCardUsePlace: request.airportCode,
            lastCardUseTimestamp: request.transactionTimestamp
        });
        return callback(null, {valid: true, message: 'User data saved for future validations'});
    }

    let [lastAirport, nextAirport] = await Promise.all([airports.get(user.lastCardUsePlace),
        airports.get(request.airportCode)]);
    if (lastAirport.code === nextAirport.code) {
        return callback(null, {valid: true, message: 'Transaction performed from the same location'});
    }

    // TODO: use moment.js
    let speed = getSpeed(lastAirport, user.lastCardUseTimestamp, nextAirport, request.transactionTimestamp);
    let valid = speed <= 13000; // 800 km/hr == ~13000 m/min
    let message = valid ? 'Transaction is OK' : 'Transaction is suspicious';

    // Update user data
    user.lastCardUsePlace = request.airportCode;
    user.lastCardUseTimestamp = request.transactionTimestamp;
    await users.set(userId, user);

    return callback(null, {valid: valid, message: message});
};

let getSpeed = (lastAirport, lastUseTimestamp, nextAirport, requestTimestamp) => {
    // Time
    let minutes = (requestTimestamp - lastUseTimestamp) / 60000;
    // Distance
    let meters = haversine(nextAirport, lastAirport, {unit: 'meter'});
    // Speed
    return meters / minutes;
};

// To run this locally:
// exports.handle({userId: 102, airportCode: 'FRA', transactionTimestamp: 1552916100000}, {}, (error, result) => {
//     console.log(error);
//     console.log(result);
// });