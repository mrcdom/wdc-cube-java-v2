# Orientacoes

A pasta resources deve ser produzida por ferramenta que transforma o projeto "frontend.react"
nos scripts que serão usados, tanto para desenvolvimento quanto para produção.

No windows, uma forma de trabalhar é usar o "mklink /H" para mapear a pasta "dist" do projeto
"frontend.react" para a pasta "META-INF/resources". Dessa maneira, o desenvolvimento pode ser
feito diretamente com o servidor do java (jetty, o default, por exemplo).