const Client = require('hazelcast-client').Client;
const ClientConfig = require('hazelcast-client').Config.ClientConfig;

let sharedHazelcastClient = null;

let createClientConfig = () => {
    let cfg = new ClientConfig();

    cfg.groupConfig.name = process.env.GROUP;
    cfg.groupConfig.password = process.env.PASSWORD;

    cfg.networkConfig.cloudConfig.enabled = true;
    cfg.networkConfig.cloudConfig.discoveryToken = process.env.DISCOVERY_TOKEN;

    cfg.properties['hazelcast.client.cloud.url'] = 'https://coordinator.hazelcast.cloud';
    cfg.properties['hazelcast.client.statistics.enabled'] = true;
    cfg.properties['hazelcast.client.statistics.period.seconds'] = 1;
    cfg.properties['hazelcast.client.heartbeat.timeout'] = 3000000;

    return cfg;
};

module.exports.getClient = async () => {
    if (!sharedHazelcastClient) {
        console.log('Creating Hazelcast client...');
        sharedHazelcastClient = await Client.newHazelcastClient(createClientConfig());
    }

    return sharedHazelcastClient;
};