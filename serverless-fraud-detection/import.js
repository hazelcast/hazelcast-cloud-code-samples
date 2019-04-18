const hazelcast = require('./hazelcast');
const aws = require('aws-sdk');

let sharedS3Client = null;

let getS3Client = () => {
    if (!sharedS3Client) {
        console.log("Creating S3 client...")
        sharedS3Client = new aws.S3();
    }

    return sharedS3Client;
};

exports.handle = async (event, context, callback) => {
    console.log('Got event: ' + JSON.stringify(event));
    context.callbackWaitsForEmptyEventLoop = false;

    let hazelcastClient = await hazelcast.getClient();
    let map = await hazelcastClient.getMap('airports');
    if (await map.isEmpty() && event.Records.length > 0) {
        let srcBucket = event.Records[0].s3.bucket.name;
        console.log('Handling upload into bucket \'' + srcBucket + '\'...');

        let srcKey = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, " "));
        let s3Client = getS3Client();
        let object = await s3Client.getObject({Bucket: srcBucket, Key: srcKey}).promise();
        let airports = JSON.parse(object.Body);
        await map.putAll(airports.map(airport => ([airport.code, airport])));
        console.log('Imported data about ' + airports.length + ' airports');

        return callback(null, true);
    }

    return callback(null, false);
};


