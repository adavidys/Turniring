<template>
  <div class="markdown-editor-field">
    <button
      class="btn-ghost markdown-editor-trigger"
      :class="{ 'is-invalid': invalid }"
      type="button"
      @click="openEditor"
    >
      {{ buttonText || tx("Відкрити markdown-редактор", "Open markdown editor") }}
    </button>

    <div v-if="hasContent" class="markdown-editor-preview markdown-render" v-html="renderedHtml"></div>
    <p v-else class="text-soft markdown-editor-preview is-empty">
      {{ tx("Текст ще не заповнено.", "Text is empty.") }}
    </p>
  </div>

  <Teleport to="body">
    <div v-if="isOpen" class="markdown-editor-backdrop" @click.self="closeEditor">
      <section
        class="markdown-editor-modal panel"
        :class="{ 'is-preview-fullscreen': isPreviewFullscreen }"
        role="dialog"
        aria-modal="true"
        :aria-label="dialogTitle"
      >
        <div class="toolbar">
          <h3 class="title-sm">{{ dialogTitle }}</h3>
          <button class="btn-ghost" type="button" @click="closeEditor">{{ tx("Закрити", "Close") }}</button>
        </div>
        <p class="text-soft">
          {{ tx("Використовуйте Markdown: #, ##, **bold**, *italic*, списки, таблиці, цитати та посилання.", "Use Markdown: #, ##, **bold**, *italic*, lists, tables, blockquotes, and links.") }}
        </p>

        <div class="markdown-editor-layout">
          <div class="markdown-editor-pane markdown-editor-input-pane">
            <div class="toolbar">
              <h4 class="title-sm">{{ tx("Редактор", "Editor") }}</h4>
              <span class="text-soft" v-if="typeof maxlength === 'number'">{{ currentLength }}/{{ maxlength }}</span>
            </div>
            <textarea
              ref="textareaRef"
              :value="modelValue"
              :placeholder="placeholder"
              :maxlength="maxlength"
              :minlength="minlength"
              @input="updateValue"
            />
          </div>

          <div class="markdown-editor-pane markdown-editor-preview-pane">
            <div class="toolbar">
              <h4 class="title-sm">{{ tx("Представлення", "Preview") }}</h4>
              <button class="btn-ghost" type="button" @click="togglePreviewFullscreen">
                {{
                  isPreviewFullscreen
                    ? tx("Вийти з повного екрана", "Exit full screen")
                    : tx("На весь екран", "Full screen")
                }}
              </button>
            </div>
            <div v-if="hasContent" class="markdown-editor-preview-body markdown-render" v-html="renderedHtml"></div>
            <p v-else class="text-soft markdown-editor-preview-empty">
              {{ tx("Напишіть текст у редакторі, щоб побачити представлення.", "Write text in the editor to see preview.") }}
            </p>
          </div>
        </div>

        <div class="toolbar">
          <button class="btn" type="button" @click="closeEditor">{{ tx("Готово", "Done") }}</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import MarkdownIt from "markdown-it";
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { tx } from "../services/i18n";

const props = defineProps({
  modelValue: {
    type: String,
    default: ""
  },
  label: {
    type: String,
    default: ""
  },
  buttonText: {
    type: String,
    default: ""
  },
  placeholder: {
    type: String,
    default: ""
  },
  maxlength: {
    type: Number,
    default: null
  },
  minlength: {
    type: Number,
    default: null
  },
  invalid: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["update:modelValue"]);
const isOpen = ref(false);
const isPreviewFullscreen = ref(false);
const textareaRef = ref(null);

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true
});

const dialogTitle = computed(() => props.label || tx("Markdown редактор", "Markdown editor"));
const currentLength = computed(() => (props.modelValue || "").length);
const hasContent = computed(() => Boolean((props.modelValue || "").trim()));
const renderedHtml = computed(() => {
  if (!hasContent.value) {
    return "";
  }
  return markdown.render(props.modelValue || "");
});

function updateValue(event) {
  emit("update:modelValue", event.target.value);
}

function openEditor() {
  isOpen.value = true;
  isPreviewFullscreen.value = false;
  nextTick(() => textareaRef.value?.focus());
}

function closeEditor() {
  isOpen.value = false;
  isPreviewFullscreen.value = false;
}

function togglePreviewFullscreen() {
  isPreviewFullscreen.value = !isPreviewFullscreen.value;
}

function onWindowKeydown(event) {
  if (event.key !== "Escape" || !isOpen.value) {
    return;
  }
  if (isPreviewFullscreen.value) {
    isPreviewFullscreen.value = false;
    return;
  }
  closeEditor();
}

watch(isOpen, (opened) => {
  if (opened) {
    window.addEventListener("keydown", onWindowKeydown);
    return;
  }
  window.removeEventListener("keydown", onWindowKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", onWindowKeydown);
});
</script>

<style scoped>
.markdown-editor-trigger {
  width: 100%;
  justify-content: center;
}

.markdown-editor-trigger.is-invalid {
  border-color: var(--danger);
  box-shadow: 0 0 0 3px rgba(255, 107, 122, 0.16);
}

.markdown-editor-preview {
  margin-top: 8px;
  border: 1px solid var(--ghost-border);
  border-radius: 14px;
  padding: 10px 12px;
  min-height: 52px;
  max-height: 112px;
  overflow: auto;
  background: var(--field-bg);
  font-size: 0.92rem;
}

.markdown-editor-preview.is-empty {
  font-style: italic;
}

.markdown-editor-backdrop {
  position: fixed;
  inset: 0;
  z-index: 85;
  display: grid;
  place-items: center;
  padding: 3vh 3vw;
  background: rgba(6, 10, 24, 0.72);
}

.markdown-editor-modal {
  width: 90vw;
  height: 90vh;
  max-width: 90vw;
  max-height: 90vh;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: 12px;
  min-height: 0;
}

.markdown-editor-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.markdown-editor-pane {
  min-height: 0;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 8px;
}

.markdown-editor-modal textarea {
  width: 100%;
  height: 100%;
  border: 1px solid var(--ghost-border);
  border-radius: 14px;
  background: var(--field-bg);
  color: var(--text);
  padding: 14px 16px;
  resize: none;
}

.markdown-editor-modal textarea:focus {
  outline: 2px solid var(--focus-outline);
  border-color: var(--focus-border);
  box-shadow: 0 0 0 4px var(--focus-ring);
}

.markdown-editor-preview-body {
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--ghost-border);
  border-radius: 14px;
  background: var(--field-bg);
  padding: 12px;
}

.markdown-editor-preview-empty {
  border: 1px dashed var(--ghost-border);
  border-radius: 14px;
  padding: 12px;
  margin: 0;
}

.markdown-editor-modal.is-preview-fullscreen .markdown-editor-input-pane {
  display: none;
}

.markdown-editor-modal.is-preview-fullscreen .markdown-editor-layout {
  grid-template-columns: minmax(0, 1fr);
}

.markdown-editor-modal.is-preview-fullscreen .markdown-editor-preview-pane {
  position: fixed;
  inset: 2vh 2vw;
  z-index: 86;
  background: var(--surface);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow);
  padding: 14px;
}

.markdown-render :deep(h1),
.markdown-render :deep(h2),
.markdown-render :deep(h3),
.markdown-render :deep(h4),
.markdown-render :deep(h5),
.markdown-render :deep(h6) {
  margin: 0.8em 0 0.35em;
  line-height: 1.2;
}

.markdown-render :deep(p),
.markdown-render :deep(ul),
.markdown-render :deep(ol),
.markdown-render :deep(blockquote),
.markdown-render :deep(pre),
.markdown-render :deep(table) {
  margin: 0 0 0.8em;
}

.markdown-render :deep(ul),
.markdown-render :deep(ol) {
  padding-left: 1.2em;
}

.markdown-render :deep(blockquote) {
  margin-left: 0;
  padding-left: 0.8em;
  border-left: 3px solid var(--line);
  color: var(--text-soft);
}

.markdown-render :deep(code) {
  padding: 0.1em 0.3em;
  border-radius: 6px;
  background: var(--code-bg);
  color: var(--code-text);
}

.markdown-render :deep(pre) {
  padding: 10px 12px;
  border-radius: 10px;
  overflow-x: auto;
  background: var(--code-bg);
}

.markdown-render :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-render :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.markdown-render :deep(th),
.markdown-render :deep(td) {
  border: 1px solid var(--line);
  padding: 6px 8px;
  text-align: left;
}

.markdown-render :deep(a) {
  text-decoration: underline;
}

@media (max-width: 960px) {
  .markdown-editor-layout {
    grid-template-columns: 1fr;
  }

  .markdown-editor-modal.is-preview-fullscreen .markdown-editor-preview-pane {
    inset: 0;
    border-radius: 0;
    border: none;
    padding: 12px;
  }
}
</style>
