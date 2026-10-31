"""Run ONCE on your PC (needs internet). Downloads ClinVar, keeps only the genes and
columns the app needs, and writes data/clinvar_subset.csv.gz (a few MB).
Optionally also pre-trains and saves a model for every gene (models/*.joblib).

    python prepare_data.py            # make the subset
    python prepare_data.py --train    # subset + pre-trained models
"""
import os, sys
import pandas as pd
import app  # reuses the exact same download / feature / training code

KEEP = ["GeneSymbol", "ClinicalSignificance", "Assembly", "VariationID", "PositionVCF", "Start",
        "ReferenceAlleleVCF", "AlternateAlleleVCF", "Type", "Name", "Chromosome"]

def main():
    print("Downloading ClinVar (large, please wait)...")
    raw = app.download_clinvar()
    genes_upper = {g.upper() for g in app.GENES}
    mask = raw["GeneSymbol"].fillna("").str.split(r"[,;|]").apply(
        lambda vs: any(v.strip().upper() in genes_upper for v in vs))
    sub = raw.loc[mask, [c for c in KEEP if c in raw.columns]]
    if "Assembly" in sub.columns:
        sub = sub[sub["Assembly"].fillna("").str.contains("GRCh38", case=False)]
    os.makedirs(os.path.dirname(app.SUBSET_PATH), exist_ok=True)
    sub.to_csv(app.SUBSET_PATH, sep="\t", index=False, compression="gzip")
    print(f"Saved {len(sub):,} rows -> {app.SUBSET_PATH} ({os.path.getsize(app.SUBSET_PATH)/1e6:.1f} MB)")
    if "--train" in sys.argv:
        for gene in app.GENES:
            try:
                _, model_df = app.prepare_gene_data(sub, gene)
                model, metrics = app.train_model(model_df)
                counts = {"usable_variants": int(len(model_df)),
                          "benign": int((model_df["Label"] == "Benign").sum()),
                          "pathogenic": int((model_df["Label"] == "Pathogenic").sum())}
                app.save_model(gene, model, metrics, counts)
                print(f"{gene}: trained, AUC={metrics['ROC-AUC']}")
            except Exception as e:
                print(f"{gene}: skipped ({e})")

if __name__ == "__main__":
    main()
