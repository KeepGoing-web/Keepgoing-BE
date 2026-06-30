package com.keepgoing.keepgoing.worker.support;

import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;

public class WorkerRedisStreamPropertiesFixture {

	public static WorkerRedisStreamProperties createDefault() {
		return new WorkerRedisStreamProperties(
				true,                     // enabled
				"test:requests",          // request stream
				"test:results",           // result stream
				"test-group",             // consumer group
				"test-consumer",          // consumer
				"test:dlq",               // DLQ stream
				3,                        // maxRetries
				60000,                    // pendingIdleTimeout
				10,                       // pendingBatchSize
				30000                     // pendingInterval (추가했다면)
		);
	}
}
