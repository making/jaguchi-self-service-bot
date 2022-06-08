SOURCE_IMAGE = os.getenv("SOURCE_IMAGE", default='ghcr.io/jaguchi/jaguchi-self-service-bot-source')
LOCAL_PATH = os.getenv("LOCAL_PATH", default='.')
NAMESPACE = os.getenv("NAMESPACE", default='default')
allow_k8s_contexts('jaguchi')
k8s_custom_deploy(
    'jaguchi-self-service-bot',
    apply_cmd="tanzu apps workload apply -f config/workload.yaml --live-update" +
               " --local-path " + LOCAL_PATH +
               " --source-image " + SOURCE_IMAGE +
               " --namespace " + NAMESPACE +
               " --build-env BP_JVM_VERSION=17 --yes >/dev/null" +
               " && kubectl get workload jaguchi-self-service-bot --namespace " + NAMESPACE + " -o yaml",
    delete_cmd="tanzu apps workload delete -f config/workload.yaml --namespace " + NAMESPACE + " --yes",
    deps=['pom.xml', './target/classes'],
    container_selector='workload',
    live_update=[
      sync('./target/classes', '/workspace/BOOT-INF/classes')
    ]
)

k8s_resource('jaguchi-self-service-bot', port_forwards=["8080:8080"],
            extra_pod_selectors=[{'serving.knative.dev/service': 'jaguchi-self-service-bot'}])
