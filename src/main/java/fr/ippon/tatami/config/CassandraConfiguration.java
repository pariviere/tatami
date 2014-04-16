package fr.ippon.tatami.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.connection.HOpTimer;
import me.prettyprint.cassandra.connection.MetricsOpTimer;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hom.EntityManagerImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import static fr.ippon.tatami.config.ColumnFamilyKeys.*;

/**
 * Cassandra configuration file.
 *
 * @author Julien Dubois
 */
@Configuration
public class CassandraConfiguration {

    private final Logger log = LoggerFactory.getLogger(CassandraConfiguration.class);

    @Inject
    private Environment env;

    private Cluster myCluster;

    @PreDestroy
    public void destroy() {
        log.info("Closing Hector connection pool");
        myCluster.getConnectionManager().shutdown();
        HFactory.shutdownCluster(myCluster);
    }

    @Bean
    public Keyspace keyspaceOperator() {
        log.info("Configuring Cassandra keyspace");
        String cassandraHost = env.getProperty("cassandra.host");
        String cassandraClusterName = env.getProperty("cassandra.clusterName");
        String cassandraKeyspace = env.getProperty("cassandra.keyspace");
        String cassandraUsername = env.getProperty("cassandra.username");
        String cassandraPassword = env.getProperty("cassandra.password");
        
        int cassandraReplicationFactor = 1;
        
        try {
        	cassandraReplicationFactor =  Integer.parseInt(env.getProperty("cassandra.replicationFactor"));
        } catch (NumberFormatException nfe) {
        	log.warn("cassandra.replicationFactor property should contains a integer. Default to 1.");
        }

        log.info("Cassandra host is '{}'", cassandraHost);
        CassandraHostConfigurator cassandraHostConfigurator = new CassandraHostConfigurator(cassandraHost);
        cassandraHostConfigurator.setMaxActive(100);
        if (env.acceptsProfiles(Constants.SPRING_PROFILE_METRICS)) {
            log.debug("Cassandra Metrics monitoring enabled");
            HOpTimer hOpTimer = new MetricsOpTimer(cassandraClusterName);
            cassandraHostConfigurator.setOpTimer(hOpTimer);
        }
        
        Map<String, String> credentials = new HashMap<String, String>();
        
        if (!StringUtils.isEmpty(cassandraUsername)) {
        	log.info("Use Cassandra authentication. Login is '{}'", cassandraUsername);
		    credentials.put("username", cassandraUsername);
		    credentials.put("password", cassandraPassword);
        }
        
        ThriftCluster cluster = new ThriftCluster(cassandraClusterName, cassandraHostConfigurator, credentials);
        
        this.myCluster = cluster; // Keep a pointer to the cluster, as Hector is buggy and can't find it again...
        ConfigurableConsistencyLevel consistencyLevelPolicy = new ConfigurableConsistencyLevel();
        consistencyLevelPolicy.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);

        KeyspaceDefinition keyspaceDef = cluster.describeKeyspace(cassandraKeyspace);
        
        if (keyspaceDef == null) {
            log.warn("Keyspace \" {} \" does not exist, creating it!", cassandraKeyspace);
            keyspaceDef = new ThriftKsDef(cassandraKeyspace);
            ((ThriftKsDef)keyspaceDef).setReplicationFactor(cassandraReplicationFactor);
            cluster.addKeyspace(keyspaceDef, true);
        }
        

        addColumnFamily(cluster, USER_CF, 0);
        addColumnFamily(cluster, FRIENDS_CF, 0);
        addColumnFamily(cluster, FOLLOWERS_CF, 0);
        addColumnFamily(cluster, STATUS_CF, 0);
        addColumnFamily(cluster, DOMAIN_CF, 0);
        addColumnFamily(cluster, REGISTRATION_CF, 0);
        addColumnFamily(cluster, RSS_CF, 0);
        addColumnFamily(cluster, MAILDIGEST_CF, 0);
        addColumnFamily(cluster, SHARES_CF, 0);
        addColumnFamily(cluster, DISCUSSION_CF, 0);
        addColumnFamily(cluster, USER_TAGS_CF, 0);
        addColumnFamily(cluster, TAG_FOLLOWERS_CF, 0);
        addColumnFamily(cluster, GROUP_MEMBERS_CF, 0);
        addColumnFamily(cluster, USER_GROUPS_CF, 0);
        addColumnFamily(cluster, GROUP_CF, 0);
        addColumnFamily(cluster, GROUP_DETAILS_CF, 0);
        addColumnFamily(cluster, ATTACHMENT_CF, 0);
        addColumnFamily(cluster, AVATAR_CF, 0);
        addColumnFamily(cluster, DOMAIN_CONFIGURATION_CF, 0);
        addColumnFamily(cluster, TATAMIBOT_CONFIGURATION_CF, 0);
        addColumnFamily(cluster, APPLE_DEVICE_CF, 0);

        addColumnFamilySortedbyUUID(cluster, TIMELINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, TIMELINE_SHARES_CF, 0);
        addColumnFamilySortedbyUUID(cluster, MENTIONLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, USERLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, USERLINE_SHARES_CF, 0);
        addColumnFamilySortedbyUUID(cluster, FAVLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, TAGLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, TRENDS_CF, 0);
        addColumnFamilySortedbyUUID(cluster, USER_TRENDS_CF, 0);
        addColumnFamilySortedbyUUID(cluster, GROUPLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, USER_ATTACHMENT_CF, 0);
        addColumnFamilySortedbyUUID(cluster, STATUS_ATTACHMENT_CF, 0);
        addColumnFamilySortedbyUUID(cluster, DOMAINLINE_CF, 0);
        addColumnFamilySortedbyUUID(cluster, DOMAIN_TATAMIBOT_CF, 0);

        addColumnFamilyCounter(cluster, COUNTER_CF, 0);
        addColumnFamilyCounter(cluster, TAG_COUNTER_CF, 0);
        addColumnFamilyCounter(cluster, GROUP_COUNTER_CF, 0);
        addColumnFamilyCounter(cluster, DAYLINE_CF, 0);

        //Tatami Bot CF
        addColumnFamily(cluster, TATAMIBOT_DUPLICATE_CF, 0);
            
        return HFactory.createKeyspace(cassandraKeyspace, cluster, consistencyLevelPolicy);
    }

    @Bean
    public EntityManagerImpl entityManager(Keyspace keyspace) {
        String[] packagesToScan = {"fr.ippon.tatami.domain", "fr.ippon.tatami.bot.config"};
        return new EntityManagerImpl(keyspace, packagesToScan);
    }
    
    private ColumnFamilyDefinition createColumnFamily(String cfName, int rowCacheKeysToSave, ComparatorType comparatorType) {
        String cassandraKeyspace = this.env.getProperty("cassandra.keyspace");
        
        ColumnFamilyDefinition cfd =
                HFactory.createColumnFamilyDefinition(cassandraKeyspace, cfName, comparatorType);
        cfd.setRowCacheKeysToSave(rowCacheKeysToSave);
        
        return cfd;
    }
    
    private void addColumnFamilyIfNotExists(ThriftCluster cluster, ColumnFamilyDefinition cfd) {
        String cassandraKeyspace = this.env.getProperty("cassandra.keyspace");
        KeyspaceDefinition keyspaceDef = cluster.describeKeyspace(cassandraKeyspace);
        
        String cfName = cfd.getName();
        
        List<String> cfNames = new ArrayList<String>();
        for (ColumnFamilyDefinition cfdef : keyspaceDef.getCfDefs()) {
        	cfNames.add(cfdef.getName());
        }
        
        if (!cfNames.contains(cfName)) {
        	log.debug("ColumnFamily " + cfName  + " does not exist. Creating it...");
            cluster.addColumnFamily(cfd);
        } 
    }
    
    private void addColumnFamily(ThriftCluster cluster, String cfName, int rowCacheKeysToSave) {
    	ColumnFamilyDefinition cfd = createColumnFamily(cfName, rowCacheKeysToSave, null);
    	addColumnFamilyIfNotExists(cluster, cfd);
    }

    private void addColumnFamilySortedbyUUID(ThriftCluster cluster, String cfName, int rowCacheKeysToSave) {
    	ColumnFamilyDefinition cfd = createColumnFamily(cfName, rowCacheKeysToSave, ComparatorType.UUIDTYPE);
    	addColumnFamilyIfNotExists(cluster, cfd);
    }


    private void addColumnFamilyCounter(ThriftCluster cluster, String cfName, int rowCacheKeysToSave) {
    	ColumnFamilyDefinition cfd = createColumnFamily(cfName, rowCacheKeysToSave, ComparatorType.UTF8TYPE);
        cfd.setDefaultValidationClass(ComparatorType.COUNTERTYPE.getClassName());
    	addColumnFamilyIfNotExists(cluster, cfd);         
    }
}
