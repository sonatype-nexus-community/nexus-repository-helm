package org.sonatype.repository.helm;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.repository.helm.internal.HelmAssetAttributePopulator;
import org.sonatype.repository.helm.internal.util.HelmDataAccess;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;

public abstract class HelmFacet
    extends FacetSupport
    implements Facet
{
  public abstract HelmDataAccess getHelmDataAccess();

  public abstract HelmAssetAttributePopulator getHelmAssetAttributePopulator();

  protected Asset findOrCreateAssetWithComponent(
      final String assetPath,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {
    Asset asset = getHelmDataAccess().findAsset(tx, bucket, assetPath);
    if (asset == null) {
      Component component = findOrCreateComponent(tx, bucket, chart);
      asset = tx.createAsset(bucket, component);
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, HELM_PACKAGE.name());
    }

    getHelmAssetAttributePopulator().populate(asset.formatAttributes(), chart);

    return asset;
  }

  protected Asset findOrCreateAssetWithFormat(
      final String assetPath,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {

    Asset asset = getHelmDataAccess().findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, HELM_PACKAGE.name());
    }

    getHelmAssetAttributePopulator().populate(asset.formatAttributes(), chart);

    return asset;
  }

  private Component findOrCreateComponent(
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {
    Component component = getHelmDataAccess().findComponent(tx, getRepository(), chart.getName(), chart.getVersion());
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(chart.getName())
          .version(chart.getVersion());
      tx.saveComponent(component);
    }
    return component;
  }
}
