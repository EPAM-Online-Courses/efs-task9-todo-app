mvn package -Dmaven.test.failure.ignore=true

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=./target/efs-task9-todo-app-1.2.0-SNAPSHOT-tests.jar \
  -DgroupId=efs -DartifactId=efs-task9-todo-app.tests \
  -Dversion=1.2.0-SNAPSHOT -Dclassifier=tests \
  -Dpackaging=test-jar \
  -DcreateChecksum=true \
  -DlocalRepositoryPath=./docs/repo
  -DgeneratePom=true

mv ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata-local.xml ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml
mv ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata-local.xml.md5 ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml.md5
mv ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata-local.xml.sha1 ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml.sha1

rm ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata-local.xml
rm ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata-local.xml.md5
rm ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata-local.xml.sha1

cp ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata.xml
cp ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml.md5 ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata.xml.md5
cp ./docs/repo/efs/efs-task9-todo-app.tests/maven-metadata.xml.sha1 ./docs/repo/efs/efs-task9-todo-app.tests/1.2.0-SNAPSHOT/maven-metadata.xml.sha1