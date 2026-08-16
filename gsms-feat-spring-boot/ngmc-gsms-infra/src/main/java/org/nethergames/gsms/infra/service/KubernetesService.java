package org.nethergames.gsms.infra.service;

import io.fabric8.kubernetes.api.model.Pod;

public interface KubernetesService {

	Pod getProxyFromName(String podName);

	void handlePodCreateInformer(Pod pod);

	void handlePodUpdateInformer(Pod oldPod, Pod newPod);

	void handlePodDeleteInformer(Pod pod);
}
