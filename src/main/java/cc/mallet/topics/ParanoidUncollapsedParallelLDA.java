package cc.mallet.topics;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.types.InstanceList;

public class ParanoidUncollapsedParallelLDA extends EfficientUncollapsedParallelLDA {

	private static final long	serialVersionUID	= 6948198361119397002L;

	public ParanoidUncollapsedParallelLDA(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	protected void samplePhi() {
		super.samplePhi();
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		System.out.println("Phi is consistent after sampling!");
		System.out.println("Topic count is consistent after sampling!");
		debugPrintMMatrix();
	}

	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		//ensureConsistentTopicTypeCounts(topicTypeCounts);
		ensureConsistentPhi(phi);
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		System.out.println("Phi is consistent after add instances!");
		System.out.println("Topic count is consistent after add instances!");
		debugPrintMMatrix();
	}

	@Override
	protected void updateCounts() throws InterruptedException {
		super.updateCounts();
		ensureConsistentPhi(phi);
		System.out.println("Phi is consistent after count update!");
		ensureConsistentTopicTypeCounts(topicTypeCountMapping);
		System.out.println("Topic count is consistent after count update!");
		debugPrintMMatrix();
	}
	
	

	@Override
	protected void postSample() {
		super.postSample();
		int updateCountSum = 0;
		for (int batch = 0; batch < batchLocalTopicTypeUpdates.length; batch++) {
				for (int topic = 0; topic < numTopics; topic++) {
					for (int type = 0; type < numTypes; type++) {
					//updateCountSum += batchLocalTopicTypeUpdates[batch][topic][type];
					updateCountSum += batchLocalTopicTypeUpdates[topic][type].get();
				}
			}
			if(updateCountSum!=0) throw new IllegalStateException("Update count does not sum to zero: " + updateCountSum); 
			updateCountSum = 0;
		}
	}

	@Override
	protected void sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		//SamplingResult res = super.sampleTopicAssignmentsParallel(tokenSequence, oneDocTopics, myBatch);
		super.sampleTopicAssignmentsParallel(ctx);
		// THIS CANNOT BE ENSURED with a job stealing implementation
		//ensureConsistentTopicTypeCountDelta(batchLocalTopicTypeUpdates, myBatch);
		//return res;
	}

}

