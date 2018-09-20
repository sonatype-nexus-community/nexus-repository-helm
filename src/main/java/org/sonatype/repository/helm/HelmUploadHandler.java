package org.sonatype.repository.helm;

import com.google.common.collect.ImmutableMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.HelmFormat;
import org.sonatype.repository.helm.internal.hosted.HelmHostedFacet;
import org.sonatype.repository.helm.internal.metadata.HelmAttributes;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;
import org.sonatype.repository.helm.internal.util.HelmPathUtils;
import org.sonatype.repository.helm.internal.util.TgzParser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.sonatype.repository.helm.internal.util.HelmDataAccess.HASH_ALGORITHMS;

@Singleton
@Named(HelmFormat.NAME)
public class HelmUploadHandler extends UploadHandlerSupport {
    private UploadDefinition definition;
    private final ContentPermissionChecker contentPermissionChecker;
    private final VariableResolverAdapter variableResolverAdapter;

    private final HelmAttributeParser helmPackageParser;

    private final HelmPathUtils helmPathUtils;

    @Inject
    public HelmUploadHandler(final ContentPermissionChecker contentPermissionChecker,final HelmPathUtils helmPathUtils,final HelmAttributeParser helmPackageParser, @Named("simple") final VariableResolverAdapter variableResolverAdapter, final Set<UploadDefinitionExtension> uploadDefinitionExtensions){
        super(uploadDefinitionExtensions);
        this.contentPermissionChecker=contentPermissionChecker;
        this.variableResolverAdapter = variableResolverAdapter;
        this.helmPackageParser = helmPackageParser;
        this.helmPathUtils = helmPathUtils;
    }


    @Override
    public UploadResponse handle(Repository repository, ComponentUpload upload) throws IOException {
        HelmHostedFacet facet = repository.facet(HelmHostedFacet.class);
        StorageFacet storageFacet = repository.facet(StorageFacet.class);


        return TransactionalStoreBlob.operation.withDb(storageFacet.txSupplier()).throwing(IOException.class).call(()->{
            //获取上传信息
            PartPayload payload = upload.getAssetUploads().get(0).getPayload();
            TempBlob tempBlob = storageFacet.createTempBlob(payload,HASH_ALGORITHMS);
            HelmAttributes attributesFromInputStream = helmPackageParser.getAttributesFromInputStream(tempBlob.get());

            String filename = upload.getAssetUploads().get(0).getPayload().getName();


            String name = attributesFromInputStream.getName();
            String version = attributesFromInputStream.getVersion();

            String extension = "tgz";

            String path = name + "-" + version + "." + extension;

            ensurePermitted(repository.getName(),HelmFormat.NAME,path, Collections.EMPTY_MAP);
            try {
                Asset upload1 = facet.upload(path, tempBlob, payload);
                return new UploadResponse(upload1);
            }finally {
            }
        });


    }



    @Override
    public UploadDefinition getDefinition() {
        if (definition == null){
            definition = getDefinition(HelmFormat.NAME,false);
        }
        return definition;
    }

    @Override
    public VariableResolverAdapter getVariableResolverAdapter() {
        return variableResolverAdapter;
    }

    @Override
    public ContentPermissionChecker contentPermissionChecker() {
        return contentPermissionChecker;
    }
}
