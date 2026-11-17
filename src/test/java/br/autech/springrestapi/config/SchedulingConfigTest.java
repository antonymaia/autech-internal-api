package br.autech.springrestapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(SchedulingConfig.class);

   @Test
   void semAPropriedadeAsTarefasNaoSaoAgendadas() {
      runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class));
   }

   @Test
   void comAPropriedadeFalseAsTarefasNaoSaoAgendadas() {
      runner.withPropertyValues("tarefas.agendadas.habilitadas=false")
         .run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class));
   }

   @Test
   void comAPropriedadeTrueAsTarefasSaoAgendadas() {
      runner.withPropertyValues("tarefas.agendadas.habilitadas=true")
         .run(ctx -> assertThat(ctx).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class));
   }
}
