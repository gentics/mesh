FROM gentics/hugo-asciidoctor

RUN yarn global add raml2html@2.2.0

COPY . /build

WORKDIR /build

# RUN rm content/blog
RUN rm content/docs ; rm content/examples ; true

COPY import/mesh/doc/src/main/docs content/docs
COPY import/mesh-enterprise/doc/src/main/docs content/docs/enterprise
COPY import/mesh-enterprise/CHANGELOG.adoc content/docs/sql-changelog.asciidoc
COPY import/mesh-enterprise/changelog-2.adoc-include content/docs/sql-changelog-2.adoc-include

# Replace changelog symlink with actual changelog file
RUN rm content/docs/changelog.asciidoc
RUN rm content/docs/changelog-2.adoc-include
RUN rm content/docs/lts-changelog.asciidoc
COPY import/mesh/CHANGELOG.adoc content/docs/changelog.asciidoc
COPY import/mesh/changelog-2.adoc-include content/docs/changelog-2.adoc-include
COPY import/mesh/LTS-CHANGELOG.adoc content/docs/lts-changelog.asciidoc

COPY import/examples content/examples

# Build CSS
RUN cd themes/gentics && yarn install && yarn build
 
# Build RAML
RUN mkdir -p static/docs/api/
RUN npx raml2html -i content/docs/api/api-docs.raml -o static/docs/api/index.html -t themes/gentics/raml/template.nunjucks

# Github Info
RUN scripts/updateGithub.sh

# Build Site
RUN hugo
