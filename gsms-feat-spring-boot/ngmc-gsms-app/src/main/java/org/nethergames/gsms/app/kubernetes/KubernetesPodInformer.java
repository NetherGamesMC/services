package org.nethergames.gsms.app.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.nethergames.gsms.infra.service.KubernetesService;
import org.springframework.stereotype.Service;

@Service
public class KubernetesPodInformer implements ResourceEventHandler<Pod> {

	private final KubernetesService commandService;

	public KubernetesPodInformer(KubernetesService commandService) {
		this.commandService = commandService;
	}

	@Override
	public void onAdd(Pod pod) {
		commandService.handlePodCreateInformer(pod);
	}

	@Override
	public void onUpdate(Pod oldPod, Pod newPod) {
		commandService.handlePodUpdateInformer(oldPod, newPod);
	}

	@Override
	public void onDelete(Pod obj, boolean deletedFinalStateUnknown) {
		commandService.handlePodDeleteInformer(obj);
	}
}
