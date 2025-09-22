{
  description = "sbt-a8 project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

        # Java 8 setup (different from other projects)
        my-java = pkgs.openjdk8_headless;
        my-scala = pkgs.scala.override { jre = my-java; };
        my-sbt = pkgs.sbt.override { jre = my-java; };
        my-ammonite = pkgs.ammonite_2_13.override { jre = my-java; };

      in {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            my-ammonite
            pkgs.exa
            my-java
            my-sbt
            my-scala
          ];
        };
      }
    );
}